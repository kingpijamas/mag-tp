package org.mag.tp.util

import scala.collection.mutable

import akka.actor.Actor

trait ChainingActor extends Actor with ReceiveChaining {
  private lazy val chainedReceives = mutable.Buffer[Receive]()

  def registerReceive(newReceive: Receive): Unit = {
    chainedReceives += newReceive
  }

  def receive: Receive = chain(chainedReceives)
}
