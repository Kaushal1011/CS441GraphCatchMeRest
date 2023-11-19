package GraphCatchMeREST

import GraphCatchMeREST.UserRegistry.ActionPerformed
import CatchMeGame.{CatchMeGameEnvironment, AgentData, Winner,GameState, ComparableNode, ConfidenceScore}
//#json-formats
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat3(User.apply)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users.apply)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed]  = jsonFormat1(ActionPerformed.apply)

  implicit val gameStateJsonFormat: RootJsonFormat[GameState] = jsonFormat3(GameState.apply)

  implicit val comparableNodeJsonFormat: RootJsonFormat[ComparableNode] = jsonFormat4(ComparableNode.apply)

  implicit val confidenceScoreJsonFormat: RootJsonFormat[ConfidenceScore] = jsonFormat2(ConfidenceScore.apply)

  implicit val agentDataJsonFormat: RootJsonFormat[AgentData] = jsonFormat5(AgentData.apply)

  implicit val actionRequestJsonFormat: RootJsonFormat[ActionRequest] = jsonFormat2(ActionRequest.apply)

  implicit val envConfigRequestJsonFormat: RootJsonFormat[EnvConfigRequest] = jsonFormat2(EnvConfigRequest.apply)

  implicit val queryGraphPathResponseJsonFormat: RootJsonFormat[queryGraphPathResponse] = jsonFormat1(queryGraphPathResponse.apply)

  implicit val agentDataRequestJsonFormat: RootJsonFormat[AgentDataRequest] = jsonFormat1(AgentDataRequest.apply)

}
//#json-formats
