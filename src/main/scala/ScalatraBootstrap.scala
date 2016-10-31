import org.mag.tp.ControllersModule
import org.mag.tp.domain.DomainModule
import org.mag.tp.ui.FrontendModule
import org.scalatra.LifeCycle

import akka.actor.ActorSystem
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with ControllersModule with DomainModule with FrontendModule {
  val system = ActorSystem("tp-mag")
  //  val workArea = createWorkArea()
  val frontendActor = createFrontendActor()

  override def init(context: ServletContext): Unit = {
    context.mount(scalatraController, "/*")
    context.mount(uiController, "/ui")
  }

  override def destroy(context: ServletContext): Unit = {
    system.terminate()
  }
}
