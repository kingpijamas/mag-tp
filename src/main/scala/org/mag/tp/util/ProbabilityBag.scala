package org.mag.tp.util

import breeze.linalg.{DenseVector, accumulate}

import scala.reflect.ClassTag
import scala.util.Random

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
  complete: Boolean)
    extends PartialFunction[T, Double] {
  import ProbabilityBag._

  def iterator: Iterator[T] = entries.iterator.map(_.value)

  def isDefinedAt(value: T): Boolean = probOf(value).isDefined  

  def apply(value: T): Double = entries.find(_.value == value).get.prob

  def probOf(value: T): Option[Double] =
    entries.find(_.value == value).map(_.prob)

  def update(value: T, prob: Double): ProbabilityBag[T] = {
    if (probOf(value).isDefined)
      transform {
        case (v, _) if v == value => prob
        case (_, prob)            => prob
      }
    else {
      val newEntries = entries.map(_.valueAndProb).toBuffer += (value -> prob)
      ProbabilityBag[T](newEntries, complete)
    }
  }

  // FIXME: there must be a better way to do this
  def transform(f: (T, Double) => Double): ProbabilityBag[T] = {
    val newEntries = entries.map {
      case Entry(_, value, prob) => (value, f(value, prob))
    }
    ProbabilityBag(newEntries, complete)
  }

  // FIXME: there must be a better way to do this
  def map[U](f: T => U): ProbabilityBag[U] = {
    val newEntries = entries.map {
      case Entry(_, value, prob) => (f(value), prob)
    }
    ProbabilityBag(newEntries, complete)
  }

  def nextValue: Option[T] = {
    val rand = Random.nextDouble
    entries.collectFirst {
      case Entry(accumProb, value, _) if accumProb > rand => value
    }
  }

  override def toString: String = s"ProbabilityBag(\n${entries.map(_.valueAndProb)})"
}
