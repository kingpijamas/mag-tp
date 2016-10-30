package org.mag.tp

import org.scalatra._

class MyScalatraServlet extends MagtpStack {

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say<a href="hello-scalate">hello to Scalate</a>
        .
      </body>
    </html>
  }

}
