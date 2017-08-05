
import javax.inject._

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}
import services.CentralBankJanitor
import v1.bank._

/**
  * Sets up custom components for Play.
  *
  * https://www.playframework.com/documentation/latest/ScalaDependencyInjection
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule {

  override def configure() = {
    bind[CentralBankJanitor].asEagerSingleton()
    bind[BankRepository].to[BankRepositoryImpl].in[Singleton]
  }
}
