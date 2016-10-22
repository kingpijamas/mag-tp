package org.tpmag

import scala.collection.mutable

import akka.actor.Actor

trait ChainingActor extends Actor {
  private lazy val chainedReceives = mutable.Buffer[Receive]()

  def registerReceive(newReceive: Receive): Unit = {
    chainedReceives += newReceive
  }

  def receive: Receive =
    chainedReceives.reduce(_ orElse _)
}
