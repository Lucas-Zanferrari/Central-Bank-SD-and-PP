package v1.bank

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import play.api.{Logger, MarkerContext}
import scala.concurrent.Future

final case class BankData(id: BankId, name: String, host: String)

class BankId private(val underlying: Int) extends AnyVal {
  override def toString: String = underlying.toString
}

object BankId {
  def apply(raw: String): BankId = {
    require(raw != null)
    new BankId(Integer.parseInt(raw))
  }
}

class BankExecutionContext @Inject()(actorSystem: ActorSystem) extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * A pure non-blocking interface for the BankRepository.
  */
trait BankRepository {
  def nextId()(implicit mc: MarkerContext): Int

  def create(data: BankData)(implicit mc: MarkerContext): Future[BankId]

  def list()(implicit mc: MarkerContext): Future[Iterable[BankData]]

  def get(id: BankId)(implicit mc: MarkerContext): Future[Option[BankData]]

  def delete(id: BankId)(implicit mc: MarkerContext): Unit
}

/**
  * A trivial implementation for the Bank Repository.
  *
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  */

@Singleton
class BankRepositoryImpl @Inject()()(implicit ec: BankExecutionContext) extends BankRepository {

  private val logger = Logger(this.getClass)
  private var idCount = 0
  private var bankList: List[BankData] = List()

  override def nextId()(implicit mc: MarkerContext): Int = {
    this.synchronized {
      idCount += 1
      idCount
    }
  }

  override def list()(implicit mc: MarkerContext): Future[Iterable[BankData]] = {
    Future {
      logger.trace(s"list: $bankList")
      bankList
    }
  }

  override def get(id: BankId)(implicit mc: MarkerContext): Future[Option[BankData]] = {
    Future {
      val bank = bankList.find(bank => bank.id == id)
      logger.trace(s"get: id = $bank")
      bank
    }
  }

  override def create(data: BankData)(implicit mc: MarkerContext): Future[BankId] = {
    Future {
      bankList.foreach(bank => {
        if (bank.name == data.name || bank.host == data.host) {
          idCount -= 1
          throw new IllegalArgumentException("Invalid bank entry")
        }
      })
      logger.trace(s"create: data = $data")
      bankList = bankList.::(data)
      data.id
    }
  }

  override def delete(id: BankId)(implicit mc: MarkerContext) {
    Future {
      logger.trace(s"delete: id = $id")
      bankList = bankList.filter(bank => bank.id != id)
    }
  }
}
