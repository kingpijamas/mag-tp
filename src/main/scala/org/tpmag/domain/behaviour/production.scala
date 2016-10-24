package org.tpmag.domain.behaviour

import scala.collection.mutable
import akka.actor.ActorRef
import org.tpmag.domain.Time
import akka.actor.Actor
import scala.util.Random
import org.tpmag.util.ChainingActor

trait ProducerActor extends ExternallyTimedActor {
  import ProductionReceiver._

  def productionReceiver: ActorRef

  def produce(): Unit = {
    println("Working")
    spendTime()
    productionReceiver ! Produce(time.get)
  }
}

object ProductionReceiver {
  case class Produce(time: Time)
}

trait ProductionReceiver extends ChainingActor {
  import ProductionReceiver._

  val producersPerTime = mutable.Map[Time, mutable.Set[ActorRef]]()

  registerReceive {
    case Produce(time) =>
      val producersForTime = producersPerTime.getOrElseUpdate(time, mutable.Set())
      producersForTime += sender
  }

  def initialTime: Time
  def time = producersPerTime.keys.foldLeft(initialTime)((t, maxT) => if (t > maxT) t else maxT)
}
