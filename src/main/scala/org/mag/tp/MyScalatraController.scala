package org.mag.tp

class MyScalatraController extends MagTpStack {

  get("/") {
    contentType="text/html"
    jade("main.jade")
  }

}
