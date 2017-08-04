package v1.bank

import javax.inject.Inject
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import play.libs.ws._
import scala.concurrent.{ExecutionContext, Future}


case class BankFormInput(name: String, host: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class BankController @Inject()(ws: WSClient, cc: BankControllerComponents)(implicit ec: ExecutionContext)
    extends BankBaseController(cc) {

  private val logger = Logger(getClass)

  private val form: Form[BankFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "name" -> nonEmptyText,
        "host" -> text
      )(BankFormInput.apply)(BankFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = BankAction.async { implicit request =>
    logger.trace("index: ")
    bankResourceHandler.find.map { banks =>
      Ok(Json.toJson(banks))
    }
  }

  def process: Action[AnyContent] = BankAction.async { implicit request =>
    logger.trace("process: ")
    processJsonBank()
  }

  def show(id: String): Action[AnyContent] = BankAction.async { implicit request =>
    logger.trace(s"show: id = $id")
    bankResourceHandler.lookup(id).map { bank =>
      Ok(Json.toJson(bank))
    }
  }

  private def processJsonBank[A]()(implicit request: BankRequest[A]): Future[Result] = {
    def failure(badForm: Form[BankFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: BankFormInput) = {
      bankResourceHandler.create(input).map { bank =>
        Created(Json.toJson(bank)).withHeaders(LOCATION -> bank.link)
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

//  def updateBankList(): Unit = {
//    bankResourceHandler.find.onComplete(tryBankResource => tryBankResource.map {
//      iterableBankResource => iterableBankResource.foreach { bankResource => removeBankIfOffline(bankResource) } })
//  }

//  perform an http request to bankData.host and if the response code indicates an error,
//  call bankRepository.delete(bankData.id)
//  def removeBankIfOffline(bankResource: BankResource): Action[AnyContent] = BankAction.async { implicit request =>
//    ws.url(bankResource.host+"/is_alive").get().map {
//      response => if(response.status == Status.OK) { bankResourceHandler.remove(BankId(bankResource.id)) }
//    }
//  }

}
