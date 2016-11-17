package org.mag.tp

import org.fusesource.scalate.util.IOUtil
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport

trait MagTpStack extends ScalatraServlet with ScalateSupport {
  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }

  get("/webjars/*") {
    val resourcePath = "/META-INF/resources/webjars/" + params("splat")
    Option(getClass.getResourceAsStream(resourcePath)) match {
      case Some(inputStream) => {
        contentType = servletContext.getMimeType(resourcePath)
        IOUtil.loadBytes(inputStream)
      }
      case _ => resourceNotFound()
    }
  }
}
