package org.mag.tp.util

import scala.reflect.ClassTag
import scala.util.Random

import breeze.linalg.DenseVector
import breeze.linalg.accumulate
import scala.collection.immutable
import scala.collection.generic.CanBuildFrom
import scala.collection.TraversableLike
import scala.collection.SetLike

object ProbabilityBag {
  case class Entry[T](accumProb: Double, value: T, prob: Double) {
    def valueAndProb: (T, Double) = (value, prob)
  }

  def complete[T](assocs: (T, Double)*): ProbabilityBag[T] = {
    ProbabilityBag(assocs, complete = true)
  }

  def partial[T](assocs: (T, Double)*): ProbabilityBag[T] = {
    ProbabilityBag(assocs, complete = false)
  }

  def apply[T](assocs: Iterable[(T, Double)], complete: Boolean = false): ProbabilityBag[T] = {
    validate(assocs.map(_._2), complete)

    val sortedAssocs = assocs.toSeq.sortBy(_._2)
    val sortedProbs = DenseVector(sortedAssocs.map(_._2): _*)
    val accumProbs = accumulate(sortedProbs).toArray(implicitly[ClassTag[Double]])
    val entries = accumProbs.zip(sortedAssocs).map {
      case (accumProb, (value, prob)) => Entry(accumProb, value, prob)
    }
    new ProbabilityBag(entries, complete)
  }

  private[this] def validate(ns: Traversable[Double], complete: Boolean): Unit = {
    def checkAllAreProbs() = {
      def isProb(d: Double) = d >= 0 && d <= 1

      ns.find(!isProb(_)).foreach { invalidNumber =>
        throw new IllegalArgumentException(s"$invalidNumber is not a probability")
      }
    }

    def checkSumIsValid() = {
      ns.sum match {
        case totalProb if totalProb < 1 && complete =>
          throw new IllegalArgumentException(s"$ns do not add up to 1. There are uncontemplated cases")
        case totalProb if totalProb > 1 =>
          throw new IllegalArgumentException(s"$ns add up to more than 1")
        case _ =>
      }
    }

    checkAllAreProbs()
    checkSumIsValid()
  }
}

class ProbabilityBag[T](
    entries: Iterable[ProbabilityBag.Entry[T]],
    complete: Boolean) {
  import ProbabilityBag._

  def iterator: Iterator[T] = entries.iterator.map(_.value)

  // TODO: there must be a better way to do this
  def map[U](f: T => U): ProbabilityBag[U] = {
    val newEntries = entries.map {
      case Entry(_, value, prob) => (f(value), prob)
    }
    ProbabilityBag(newEntries, complete)
  }

  def getRand: Option[T] = {
    val rand = Random.nextDouble
    entries.collectFirst {
      case Entry(accumProb, value, _) if accumProb > rand => value
    }
  }
}
