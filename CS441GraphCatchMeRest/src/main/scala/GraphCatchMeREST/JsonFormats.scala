package GraphCatchMeREST

import CatchMeGame.{AgentData,GameState, ComparableNode, ConfidenceScore}
//#json-formats
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol

// serializing and deserializing case classes
// will convert to and from JSON
object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

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
