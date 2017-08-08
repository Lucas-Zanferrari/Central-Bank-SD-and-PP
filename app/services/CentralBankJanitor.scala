package services

import v1.bank.BankResourceHandler
import play.api.libs.ws.WSClient
import javax.inject.Inject
import play.api.Logger
import akka.actor.ActorSystem
import play.api.http.Status
import scala.concurrent.{Await, ExecutionContext, TimeoutException}
import scala.concurrent.duration._

class CentralBankJanitor @Inject() (ws: WSClient, bankResourceHandler: BankResourceHandler, actorSystem: ActorSystem)(implicit ec: ExecutionContext) {

  private val START_AFTER: FiniteDuration = 10.seconds
  private val REPEAT_AFTER: FiniteDuration = 15.minutes
  private val REQUEST_TIMEOUT: FiniteDuration = 30.seconds
  private val PROTOCOL = "http://"

  actorSystem.scheduler.schedule(initialDelay = START_AFTER, interval = REPEAT_AFTER) {
    val result = bankResourceHandler.find.map {
      bankIterable => bankIterable.foreach {
        bankResource => {
          println(s"${getClass.getCanonicalName}: checking if bank #${bankResource.id} at ${bankResource.host} is online")
          ws.url(s"$PROTOCOL${bankResource.host}/v1/alive").withRequestTimeout(REQUEST_TIMEOUT).get().map {
            response =>
              if (response.status != Status.OK) {
                Logger.info(s"${getClass.getCanonicalName}: bank #${bankResource.id} will be removed")
                bankResourceHandler.remove(bankResource.id)
              }
              else
                Logger.info(s"${getClass.getCanonicalName}: bank #${bankResource.id} is still online")
          }
          .recover {
            case e: TimeoutException => {
              Logger.info(s"${getClass.getCanonicalName}: bank #${bankResource.id} will be removed due to timeout: ${e.getMessage}")
              bankResourceHandler.remove(bankResource.id)
            }
            case e: Exception => {
              Logger.info(s"${getClass.getCanonicalName}: bank #${bankResource.id} will be removed: ${e.getMessage}")
              bankResourceHandler.remove(bankResource.id)
            }
          }
        }
      }
    }

    Await.result(result, Duration.Inf)
  }

}
