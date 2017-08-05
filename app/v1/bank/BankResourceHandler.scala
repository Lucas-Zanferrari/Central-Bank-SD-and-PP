package v1.bank

import javax.inject.{Inject, Provider}
import play.api.MarkerContext
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._


/**
  * DTO for displaying bank information.
  */
case class BankResource(id: String, link: String, name: String, host: String)

object BankResource {

  /**
    * Mapping to write a BankResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[BankResource] {
    def writes(bank: BankResource): JsValue = {
      Json.obj(
        "id" -> bank.id,
        "link" -> bank.link,
        "name" -> bank.name,
        "host" -> bank.host
      )
    }
  }
}

/**
  * Controls access to the backend data, returning [[BankResource]]
  */
class BankResourceHandler @Inject()(
    routerProvider: Provider[BankRouter],
    bankRepository: BankRepository)(implicit ec: ExecutionContext) {

  def create(bankInput: BankFormInput)(implicit mc: MarkerContext): Future[BankResource] = {
    val nextId = bankRepository.nextId()
    val data = BankData(BankId(nextId.toString), bankInput.name, bankInput.host)
    bankRepository.create(data).map { id =>
      createBankResource(data)
    }
  }

  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[BankResource]] = {
    val bankFuture = bankRepository.get(BankId(id))
    bankFuture.map { maybeBankData =>
      maybeBankData.map { bankData =>
        createBankResource(bankData)
      }
    }
  }

  def find(implicit mc: MarkerContext): Future[Iterable[BankResource]] = {
    bankRepository.list().map { bankDataList =>
      bankDataList.map(bankData => createBankResource(bankData))
    }
  }

  def remove(id: String)(implicit mc: MarkerContext): Unit = {
    bankRepository.delete(BankId(id))
  }

  private def createBankResource(b: BankData): BankResource = {
    BankResource(b.id.toString, routerProvider.get.link(b.id), b.name, b.host)
  }

}
