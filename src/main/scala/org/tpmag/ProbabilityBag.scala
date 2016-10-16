package org.tpmag

import scala.reflect.ClassTag
import scala.util.Random

import org.tpmag.ProbabilityBag.Entry

import ProbabilityBag.Entry
import breeze.linalg.DenseVector
import breeze.linalg.accumulate
import collection.immutable

object ProbabilityBag {
  def complete[T](assocs: (Double, T)*): ProbabilityBag[T] = {
    ProbabilityBag(assocs, complete = true)
  }

  def partial[T](assocs: (Double, T)*): ProbabilityBag[T] = {
    ProbabilityBag(assocs, complete = false)
  }

  def apply[T](assocs: Traversable[(Double, T)], complete: Boolean = false): ProbabilityBag[T] = {
    validate(assocs.map(_._1), complete)

    val sortedAssocs = assocs.toSeq.sortBy(_._1)
    val sortedProbs = DenseVector(sortedAssocs.map(_._1): _*)
    val accumProbs = accumulate(sortedProbs).toArray(implicitly[ClassTag[Double]])
    val entries = accumProbs.zip(sortedAssocs).map {
      case (accumProb, (prob, value)) => Entry(accumProb, prob, value)
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

  case class Entry[T](accumProb: Double, prob: Double, value: T)
}

class ProbabilityBag[T](entries: Traversable[Entry[T]], complete: Boolean) {
  // TODO extends immutable.Map[T, Double] {

  def getRand: Option[T] = {
    val rand = Random.nextDouble
    entries.collectFirst {
      case Entry(accumProb, _, value) if accumProb > rand => value
    }
  }
}
