package GraphCatchMeREST

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import CatchMeGame.{CatchMeGameEnvironment, AgentData, Winner,ComparableNode}


// registry actor as created by refering to the akka http boilerplate
object CatchMeGameRegistry {

  final case class getAgentData(agentName: String ,replyTo: ActorRef[AgentData]) extends Command
  final case class agentAction(agentName: String,moveToId:Int, replyTo: ActorRef[CatchMeGameEnvironment]) extends Command
  final case class initializeGame(regionalGraphPath:String, queryGraphPath:String,replyTo: ActorRef[CatchMeGameEnvironment]) extends Command
  final case class resetGame(replyTo: ActorRef[CatchMeGameEnvironment]) extends Command
  final case class resetParamsGame(regionalGraphPath:String, queryGraphPath:String,replyTo: ActorRef[CatchMeGameEnvironment]) extends Command
  final case class getQueryGraph(replyTo: ActorRef[String]) extends Command

  sealed trait Command
  def apply(): Behavior[Command] = registry(CatchMeGameEnvironment())

  private def registry(gameEnvironment: CatchMeGameEnvironment): Behavior[Command] =
    Behaviors.receiveMessage {
      case getAgentData(agentName,replyTo) =>
        replyTo ! gameEnvironment.getAgentData(agentName)
        Behaviors.same
      case agentAction(agentName,moveToId, replyTo) =>
        val envRet = gameEnvironment.action(agentName,moveToId)
        replyTo ! envRet
        registry(envRet)
      case initializeGame(regionalGraphPath, queryGraphPath,replyTo) =>
        val envRet = gameEnvironment.init(regionalGraphPath, queryGraphPath)
        replyTo ! envRet
        registry(envRet)
      case resetGame(replyTo) =>
        val envRet = gameEnvironment.reset()
        replyTo ! envRet
        registry(envRet)
      case resetParamsGame(regionalGraphPath, queryGraphPath,replyTo) =>
        val envRet = gameEnvironment.reset(regionalGraphPath, queryGraphPath)
        replyTo ! envRet
        registry(envRet)
      case getQueryGraph(replyTo) =>
        replyTo ! gameEnvironment.queryGraphPath.getOrElse("")
        Behaviors.same
    }


}
