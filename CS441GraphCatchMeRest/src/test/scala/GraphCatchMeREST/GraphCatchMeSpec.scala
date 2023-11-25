package GraphCatchMeREST

import CatchMeGame.{AgentData, ComparableNode, GameState}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GraphCatchMeSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[_] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val catchMeGameRegistry = testKit.spawn(CatchMeGameRegistry())
  lazy val routes = new CatchMeRoutes(catchMeGameRegistry).gameRoutes

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._


  "CatchMeRoutes" should {

    "Post request init game without valid graph paths should have -1" in {
      val envConfigRequest = EnvConfigRequest("", "")
      val envConfigRequestEntity = Marshal(envConfigRequest).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = Post("/init").withEntity(envConfigRequestEntity)
      val credentials = HttpCredentials.createOAuth2BearerToken("ifilosemyselftonightiwillblamescala")
      request ~> addCredentials(credentials) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[GameState]
        e.policeLoc should ===(-1)
        e.thiefLoc should ===(-1)
      }
    }

    "Post request query agent state without proper init " in {
      val agentDataRequest = AgentDataRequest("police")
      val agentDataRequestEntity = Marshal(agentDataRequest).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = Post("/queryagent").withEntity(agentDataRequestEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[AgentData]
        e.name should ===("null")
        e.valuableDataDistance should ===(0)
        e.currentLocation should ===(ComparableNode(-1, List(), List()))
      }
    }

    "Post request query agent with proper init" in {
      val envConfigRequest = EnvConfigRequest("https://buckerforsimrank.s3.amazonaws.com/input/edges.txt", "https://buckerforsimrank.s3.amazonaws.com/input/edgesPerturbed.txt")
      val envConfigRequestEntity = Marshal(envConfigRequest).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = Post("/init").withEntity(envConfigRequestEntity)
      val credentials = HttpCredentials.createOAuth2BearerToken("ifilosemyselftonightiwillblamescala")
      request ~> addCredentials(credentials) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[GameState]
        e.policeLoc should not equal(-1)
        e.thiefLoc should not equal(-1)
      }
      val agentDataRequest = AgentDataRequest("police")
      val agentDataRequestEntity = Marshal(agentDataRequest).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request2 = Post("/queryagent").withEntity(agentDataRequestEntity)
      request2 ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[AgentData]
        e.name should ===("police")
        e.currentLocation should not equal (ComparableNode(-1, List(), List()))
      }
    }

    "Get query graph path in " in {
      val request = Get("/querygraph")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[queryGraphPathResponse]
        e.queryGraphPath should not equal ("")
      }
    }

    "Post an action request" in {

      val  actionRequest = ActionRequest("police", 1)

      val actionRequestEntity = Marshal(actionRequest).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      val request = Post("/action").withEntity(actionRequestEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[GameState]
        e.policeLoc should not equal(-1)
        e.thiefLoc should not equal(-1)
      }

    }

    "Post a reset request" in {
      val envConfigRequest = EnvConfigRequest("https://buckerforsimrank.s3.amazonaws.com/input/edges.txt", "https://buckerforsimrank.s3.amazonaws.com/input/edgesPerturbed.txt")
      val envConfigRequestEntity = Marshal(envConfigRequest).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = Post("/reset").withEntity(envConfigRequestEntity)
      val credentials = HttpCredentials.createOAuth2BearerToken("ifilosemyselftonightiwillblamescala")
      request ~> addCredentials(credentials) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val e = entityAs[GameState]
        e.policeLoc should not equal(-1)
        e.thiefLoc should not equal(-1)
      }
    }

  }

}
