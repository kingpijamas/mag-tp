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
    val maxDeviationsAllowed: Double,
    val employeeCount: Int) extends Actor {
  import ProductionWatcher._

  val producersPerTime: mutable.Map[Time, mutable.Set[ActorRef]] = mutable.Map()

  def receive = {
    case Produce(time) => {
      val producersForTime = producersPerTime.getOrElseUpdate(time, mutable.Set())
      producersForTime += sender
    }
    case FireLazies => {
      if (!producersPerTime.isEmpty) {
        val registeredTimes = producersPerTime.keys.toSeq
        val periodTimes = registeredTimes.sorted.take(periodLength)
        val (from, to) = (periodTimes.min, periodTimes.max)
        println(s"lazies: ${lazies(from, to).size}")
        producersPerTime -= (from, to)
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
      case (producer, produce) if Math.abs(produce - meanProduce) > tolerance => producer
    }
  }
}
