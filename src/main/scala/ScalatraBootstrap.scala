import org.mag.tp.MyScalatraServlet
import org.mag.tp.domain.DomainModule
import org.scalatra.LifeCycle

import akka.actor.ActorSystem
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with DomainModule {
  val system = ActorSystem("tp-mag")
  val workArea = createWorkArea()

  override def init(context: ServletContext) {
    context.mount(new MyScalatraServlet, "/*")
  }

  override def destroy(context: ServletContext): Unit = {
    system.terminate()
  }
}
