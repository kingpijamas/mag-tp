package org.tpmag.domain.behaviour

import akka.actor.Terminated
import scala.util.Random
import akka.actor.ActorRef
import akka.actor.Actor
import java.lang.Math.min

import scala.collection.mutable

object Socialization {
  val MaxRelation = 1

  case object Talk
  case class TalkBack(newRelation: Double)
}

trait Socialization extends ExternallyTimedActor {
  import Socialization._

  val relations = mutable.Map[ActorRef, Double](self -> MaxRelation)

  def socialPool: ActorRef

  def socialize(): Unit = {
    println("Blah blah")
    socialPool ! Talk
  }

  def respondToSocialization: Receive = {
    case Talk if sender == self =>
      recoverTime()

    case Talk if sender != self =>
      val relation: Double = relations.getOrElse(sender, 0) // FIXME: magic number!
      val newRelation = min(relation + Random.nextDouble, 1) // FIXME: magic number!
      relations(sender) = newRelation
      context.watch(sender)
      sender ! TalkBack(newRelation)

    case TalkBack(newRelation) =>
      relations(sender) = newRelation
      context.watch(sender)
      println(s"$self $relations")

    case Terminated(employee) =>
      relations -= sender
      println("Goodbye friend!")
  }
}
