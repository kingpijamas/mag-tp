import javax.servlet.ServletContext

import akka.actor.ActorSystem
import org.mag.tp.controller.ControllersModule
import org.mag.tp.domain.DomainModule
import org.mag.tp.ui.FrontendModule
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle with ControllersModule with DomainModule with FrontendModule {
  val system = ActorSystem("tp-mag")
  val frontendActor = createFrontendActor()

  override def init(context: ServletContext): Unit = {
    context.mount(uiController, "/*")
  }

  override def destroy(context: ServletContext): Unit = {
    system.terminate()
  }
}
