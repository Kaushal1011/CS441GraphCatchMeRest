package helpers

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.Base64
import winner.winner.{WinnerRequest, WinnerResponse}


class LambdaInvoker(implicit  system: ActorSystem[Nothing],ec: ExecutionContext,  url : String) {

  // Function to generate base64 encoded protobuf data
  private def generateBase64EncodedProtobuf(agentName: String): String = {
    val winnerRequest = WinnerRequest(agentName)
    val serializedData = winnerRequest.toByteArray
    Base64.getEncoder.encodeToString(serializedData)
  }

  // Function to make HTTP POST request
  private def invokeLambdaViaHttp(encodedData: String): Future[HttpResponse] = {
    val entity = HttpEntity.Strict(ContentTypes.`application/grpc+proto`, ByteString(encodedData))
    val request = HttpRequest(method = HttpMethods.POST, uri = url, entity = entity)
    Http().singleRequest(request)
  }

  // Function to decode protobuf response
  private def decodeResponse(data: String): WinnerResponse = {
    val decodedBytes = Base64.getDecoder.decode(data)
    WinnerResponse.parseFrom(decodedBytes)
  }

  // Public method to invoke the Lambda function and get the response
  def invokeLambdaAndGetResponse(agentName: String): Future[String] = {
    val base64EncodedData = generateBase64EncodedProtobuf(agentName)

    invokeLambdaViaHttp(base64EncodedData).flatMap { response =>
      response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { byteString =>
        val responseString = byteString.utf8String
        try {
          val winnerResponse = decodeResponse(responseString)
          s"Success: $winnerResponse"
        } catch {
          case e: Exception => s"Error in decoding response: ${e.getMessage}"
        }
      }
    }.recover {
      case e: Exception => s"Error in HTTP request: ${e.getMessage}"
    }
  }
}
