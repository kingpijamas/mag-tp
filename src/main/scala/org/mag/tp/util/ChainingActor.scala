package org.mag.tp.util

import akka.actor.Actor

import scala.collection.mutable

trait ChainingActor extends Actor with ReceiveChaining {
  private lazy val chainedReceives = mutable.Buffer[Receive]()

  def registerReceive(newReceive: Receive): Unit = {
    chainedReceives += newReceive
  }

  def receive: Receive = chain(chainedReceives)
}
