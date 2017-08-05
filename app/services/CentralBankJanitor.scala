package services

import v1.bank.BankResourceHandler
import play.api.libs.ws.WSClient
import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.http.Status

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class CentralBankJanitor @Inject() (ws: WSClient, bankResourceHandler: BankResourceHandler, actorSystem: ActorSystem)(implicit ec: ExecutionContext) {

  private val startAfter: FiniteDuration = 10.seconds
  private val repeatAfter: FiniteDuration = 15.minutes
  private val requestTimeOut: FiniteDuration = 30.seconds
  private val protocol = "http://"

  actorSystem.scheduler.schedule(initialDelay = startAfter, interval = repeatAfter) {
    val result = bankResourceHandler.find.map {
      bankIterable => bankIterable.foreach {
        bankResource => {
          println(s"${getClass.getCanonicalName}: checking if bank #${bankResource.id} at ${bankResource.host} is online")
          ws.url(s"$protocol${bankResource.host}/is_alive").withRequestTimeout(requestTimeOut).get().map {
            response =>
              if (response.status != Status.OK) {
                println(s"${getClass.getCanonicalName}: bank #${bankResource.id} will be removed")
                bankResourceHandler.remove(bankResource.id)
              }
          }.recover {
            case e: scala.concurrent.TimeoutException => {
              println(s"${getClass.getCanonicalName}: bank #${bankResource.id} will be removed")
              bankResourceHandler.remove(bankResource.id)
            }
          }
        }
      }
    }

    Await.result(result, Duration.Inf)
  }

}
