package GraphCatchMeREST

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import CatchMeGame.{AgentData, CatchMeGameEnvironment, ComparableNode, GameState, Winner}
import akka.http.scaladsl.server.directives.Credentials

// case classes to handle request response data
case class ActionRequest(agentName: String, moveToId: Int)
case class EnvConfigRequest(regionalGraphPath: String, queryGraphPath: String)
case class AgentDataRequest(agentName: String)
case class queryGraphPathResponse(queryGraphPath: String)

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import Directives._
import akka.http.scaladsl.server.ExceptionHandler

// routes class as created by refering to the akka http boilerplate
class CatchMeRoutes (catchMeRegistry: ActorRef[CatchMeGameRegistry.Command])(implicit val system: ActorSystem[_]) {

  implicit val myExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: Exception =>
      extractUri { uri =>
        system.log.error(s"Request to $uri could not be handled normally")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = "An error occurred: " + ex.getMessage))
      }
  }

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  // all the functions that will be called by the routes
  // they call the actor and return the result
  // ask replyTo pattern
  def getAgentData(agentName: String): Future[AgentData] = {
     system.log.info("getAgentData: " + agentName)
     catchMeRegistry.ask(CatchMeGameRegistry.getAgentData(agentName, _))
  }

  def agentAction(agentName: String,moveToId:Int): Future[CatchMeGameEnvironment] = {
    system.log.info("agentAction: " + agentName + " moveToId: " + moveToId)
    catchMeRegistry.ask(CatchMeGameRegistry.agentAction(agentName, moveToId, _))
  }

  def initializeGame(regionalGraphPath:String, queryGraphPath:String): Future[CatchMeGameEnvironment] = {
    system.log.info("initializeGame: " + regionalGraphPath + " queryGraphPath: " + queryGraphPath)
    catchMeRegistry.ask(CatchMeGameRegistry.initializeGame(regionalGraphPath, queryGraphPath, _))
  }

  def resetGame(): Future[CatchMeGameEnvironment] = {
    system.log.info("resetGame")
    catchMeRegistry.ask(CatchMeGameRegistry.resetGame(_))
  }

  def resetParamsGame(regionalGraphPath:String, queryGraphPath:String): Future[CatchMeGameEnvironment] = {
    system.log.info("resetParamsGame: " + regionalGraphPath + " queryGraphPath: " + queryGraphPath)
    catchMeRegistry.ask(CatchMeGameRegistry.resetParamsGame(regionalGraphPath, queryGraphPath, _))
  }

  def getQueryGraph(): Future[String] = {
    system.log.info("getQueryGraph")
    catchMeRegistry.ask(CatchMeGameRegistry.getQueryGraph(_))
  }


  private def getGameState(env: CatchMeGameEnvironment): GameState = {

    system.log.info("getGameState")

    if (env.winner.getOrElse(Winner.NoOne) == Winner.Police) {
      GameState(env.currentPoliceNode.asInstanceOf[ComparableNode].id, env.currentThiefNode.asInstanceOf[ComparableNode].id, "Police")
    }
    else if (env.winner.getOrElse(Winner.NoOne) == Winner.Thief) {
      GameState(env.currentPoliceNode.asInstanceOf[ComparableNode].id, env.currentThiefNode.asInstanceOf[ComparableNode].id, "Thief")
    }
    else {
      GameState(env.currentPoliceNode.id, env.currentThiefNode.id, "None")
    }

  }



  //  val secretToken = "ifilosemyselftonightiwillblamescala"
  // load this from config
  val secretToken = system.settings.config.getString("my-app.server.secret-key")

  // token authentication for init and reset
  def myTokenAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case Credentials.Provided(token) if token == secretToken => Some("Authorised")
      case _ => None
    }
  }

  // the routes
  lazy val gameRoutes: Route = handleExceptions(myExceptionHandler) {
    concat(
      path("action") {
        post {
          entity(as[ActionRequest]) { actionRequest =>
            onSuccess(agentAction(actionRequest.agentName, actionRequest.moveToId)) { performed =>
              // Here you would call the method of the gameEnvironment and return some data
              val gs = getGameState(performed)
              complete(gs)
            }
          }
        }
      },
      path("init") {
        post {
          authenticateOAuth2("secureSite", myTokenAuthenticator) { envConfigRequest =>
            if (envConfigRequest == "Authorised") {
              entity(as[EnvConfigRequest]) { configRequest =>
                onSuccess(initializeGame(configRequest.regionalGraphPath, configRequest.queryGraphPath)) { performed =>
                  // Here you would call the method of the gameEnvironment and return some data
                  val gs = getGameState(performed)
                  complete(gs)
                }

              }
            }
            else {
              complete("Not Authorised")
            }
          }
        }
      },
      path("reset") {
        authenticateOAuth2("secureSite", myTokenAuthenticator) { envConfigRequest =>
          if (envConfigRequest == "Authorised") {
            post {
              entity(as[EnvConfigRequest]) { configRequest =>
                onSuccess(resetParamsGame(configRequest.regionalGraphPath, configRequest.queryGraphPath)) { performed =>
                  // Here you would call the method of the gameEnvironment and return some data
                  val gs = getGameState(performed)
                  complete(gs)
                }
              }
            }
          }
          else {
            complete("Not Authorised")
          }
        }
      },
      path("querygraph") {
        get {
          onSuccess(getQueryGraph()) { queryGraphPath =>
            complete(queryGraphPathResponse(queryGraphPath))
          }
        }
      },
      path("queryagent") {
        post {
          entity(as[AgentDataRequest]) { agentDataRequest =>
            onSuccess(getAgentData(agentDataRequest.agentName)) { agentData =>
              complete(agentData)
            }
          }
        }
      },
    )
  }
}
