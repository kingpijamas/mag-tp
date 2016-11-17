package org.mag.tp.util

import scala.math.sqrt

object Stats {
  sealed trait StatsType

  private case class PartialStats[N: Numeric](
    min: Option[N] = None,
    max: Option[N] = None,
    mean: Option[Double] = None,
    sum: N,
    count: Long) extends StatsType

  case class FullStats[N: Numeric](
    min: Option[N] = None,
    max: Option[N] = None,
    mean: Option[Double] = None,
    variance: Option[Double] = None,
    stdDev: Option[Double] = None,
    sum: N,
    count: Long) extends StatsType

  def full[N: Numeric](values: Traversable[N]): FullStats[N] =
    partial(values) match {
      case PartialStats(_, _, _, sum, 0) =>
        FullStats(sum = sum, count = 0)

      case PartialStats(min, max, Some(mean), sum, count) =>
        val variance = varianceOf(values, mean, count)
        val stdDev = sqrt(variance)
        // TODO: use shapeless!
        FullStats(
          min = min,
          max = max,
          sum = sum,
          count = count,
          mean = Some(mean),
          variance = Some(variance),
          stdDev = Some(stdDev))
    }

  private[this] def partial[N: Numeric](values: TraversableOnce[N]): PartialStats[N] = {
    val ev = implicitly[Numeric[N]]

    val results = values.foldLeft(PartialStats(min = None, max = None, sum = ev.zero, count = 0)) {
      case (PartialStats(min, max, _, sum, count), value) =>
        PartialStats(
          min = min.map(ev.min(_, value)).orElse(Some(value)),
          max = max.map(ev.max(_, value)).orElse(Some(value)),
          sum = ev.plus(sum, value),
          count = count + 1)
    }

    if (results.count == 0) {
      results
    } else {
      val sum = ev.toDouble(results.sum)
      results.copy(mean = Some(sum / results.count))
    }
  }

  private[this] def varianceOf[N: Numeric](values: Traversable[N], mean: Double, count: Long): Double =
    values.foldLeft(0D) { (acc, value) => acc + sqDiff(value, mean) } / count

  private[this] def sqDiff[N: Numeric](x: N, y: Double): Double = {
    val ev = implicitly[Numeric[N]]
    val diff = ev.toDouble(x) - y
    diff * diff
  }
}
