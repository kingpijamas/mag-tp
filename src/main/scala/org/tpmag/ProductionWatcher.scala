package org.tpmag

import scala.annotation.migration
import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorRef
import breeze.linalg.support.CanTraverseValues
import breeze.linalg.support.CanTraverseValues.ValuesVisitor
import breeze.numerics.sqrt
import breeze.stats.MeanAndVariance
import breeze.stats.meanAndVariance

object ProductionWatcher {
  case class Produce(time: Time)
  case object FireLazies

  // Needed for statistics
  implicit object IterableAsTraversable extends CanTraverseValues[Iterable[Double], Double] {
    def traverse(from: Iterable[Double], fn: ValuesVisitor[Double]): Unit = from.map(fn.visit)
    def isTraversableAgain(from: Iterable[Double]): Boolean = true
  }
}

class ProductionWatcher(
    val periodLength: Int,
    val maxDeviationsAllowed: Int,
    val employeeCount: Int) extends Actor {
  import ProductionWatcher._

  val producersPerTime: mutable.Map[Time, mutable.Set[ActorRef]] = mutable.Map().withDefault(_ => mutable.Set())

  def receive = {
    case Produce(time) => { producersPerTime(time) += sender; producersPerTime.size }
    case FireLazies => {
      if (!producersPerTime.isEmpty) {
        println("Hola")
        val registeredTimes = producersPerTime.keys.toSeq
        val period = registeredTimes.sorted(Ordering[Time].reverse).take(2)
        val (from, to) = (period(0), period(1))
        producersPerTime.-=(from, to)
        println(s"Analyzing period ($from, $to)...")
        val asd = lazies(from, to)
        println(s"lazies: $asd")
      }
    }
  }

  private[this] def producersBetween(from: Time, to: Time): Iterable[ActorRef] =
    producersPerTime.collect { case (t, producers) if t >= from && t < to => producers }.flatten

  private[this] def lazies(from: Time, to: Time) = {
    val relevantProducers = producersBetween(from, to)
    val producePerProducer = relevantProducers.groupBy(identity).mapValues(_.size.toDouble)
    val MeanAndVariance(meanProduce, produceVariance, _) = meanAndVariance(producePerProducer.values)
    val tolerance = maxDeviationsAllowed * sqrt(produceVariance)

    producePerProducer.collect {
      case (producer, produce) if Math.abs(produce - meanProduce) < tolerance => producer
    }
  }
}
