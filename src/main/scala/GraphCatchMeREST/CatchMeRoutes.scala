package GraphCatchMeREST

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import GraphCatchMeREST.UserRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import CatchMeGame.{AgentData, CatchMeGameEnvironment, ComparableNode, GameState, Winner}
import akka.http.scaladsl.server.directives.Credentials


case class ActionRequest(agentName: String, moveToId: Int)
case class EnvConfigRequest(regionalGraphPath: String, queryGraphPath: String)
case class AgentDataRequest(agentName: String)
case class queryGraphPathResponse(queryGraphPath: String)




class CatchMeRoutes (catchMeRegistry: ActorRef[CatchMeGameRegistry.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getAgentData(agentName: String): Future[AgentData] = {
     catchMeRegistry.ask(CatchMeGameRegistry.getAgentData(agentName, _))

  }

  def agentAction(agentName: String,moveToId:Int): Future[CatchMeGameEnvironment] = {
    catchMeRegistry.ask(CatchMeGameRegistry.agentAction(agentName, moveToId, _))
  }

  def initializeGame(regionalGraphPath:String, queryGraphPath:String): Future[CatchMeGameEnvironment] =
    catchMeRegistry.ask(CatchMeGameRegistry.initializeGame(regionalGraphPath, queryGraphPath, _))
  def resetGame(): Future[CatchMeGameEnvironment] =
    catchMeRegistry.ask(CatchMeGameRegistry.resetGame(_))
  def resetParamsGame(regionalGraphPath:String, queryGraphPath:String): Future[CatchMeGameEnvironment] =
    catchMeRegistry.ask(CatchMeGameRegistry.resetParamsGame(regionalGraphPath, queryGraphPath, _))
  def getQueryGraph(): Future[String] =
    catchMeRegistry.ask(CatchMeGameRegistry.getQueryGraph(_))


  private def getGameState(env: CatchMeGameEnvironment): GameState = {

    if (env.winner.getOrElse(Winner.NoOne) == Winner.Police) {
      GameState(env.currentPoliceNode.asInstanceOf[ComparableNode].id, env.currentThiefNode.asInstanceOf[ComparableNode].id, "Police")
    }
    else if (env.winner.getOrElse(Winner.NoOne) == Winner.Thief) {
      GameState(env.currentPoliceNode.asInstanceOf[ComparableNode].id, env.currentThiefNode.asInstanceOf[ComparableNode].id, "Thief")
    }
    else {
      GameState(env.currentPoliceNode.getOrElse(ComparableNode()).id, env.currentThiefNode.getOrElse(ComparableNode()).id, "None")
    }

  }



//  val secretToken = "ifilosemyselftonightiwillblamescala"
  // load this from config
  val secretToken = "ifilosemyselftonightiwillblamescala"

  def myTokenAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case Credentials.Provided(token) if token == secretToken => Some("Authorised")
      case _ => None
    }
  }

  lazy val gameRoutes: Route = concat(
    path("action") {
      post {
        entity(as[ActionRequest]) { actionRequest =>
          onSuccess (agentAction(actionRequest.agentName, actionRequest.moveToId)) { performed =>
            // Here you would call the method of the gameEnvironment and return some data
            val gs = getGameState(performed)
            complete(gs)
          }
        }
      }
    },
    path("init") {
      post {
        authenticateOAuth2("secureSite",  myTokenAuthenticator) { envConfigRequest =>
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
