package org.tpmag.domain.behaviour

import akka.actor.Terminated
import scala.util.Random
import akka.actor.ActorRef
import akka.actor.Actor
import java.lang.Math.min

import scala.collection.mutable

object SocialActor {
  val MaxRelation = 1
  val FriendshipPoint = 0.5

  case object Talk
  case class TalkBack(newRelation: Double)
}

trait SocialActor extends ExternallyTimedActor {
  import SocialActor._

  val relations = mutable.Map[ActorRef, Double](self -> MaxRelation).withDefaultValue(0)

  def socialPool: ActorRef

  def socialize(): Unit = {
    println(s"$self: Blah blah")
    spendTime()
    socialPool ! Talk
  }

  def respondToSocialization: Receive = {
    case Talk if sender == self =>
      recoverTime()

    case Talk if sender != self =>
      val newRelation = min(relations(sender) + Random.nextDouble, 1) // FIXME: magic number!
      relations(sender) = newRelation
      context.watch(sender)
      sender ! TalkBack(newRelation)

    case TalkBack(newRelation) =>
      relations(sender) = newRelation
      context.watch(sender)
      println(s"$self: Hello back!") // $relations

    case Terminated(employee) =>
      relations -= sender
      println(s"$self: Goodbye friend!")
  }

  def isFriend(other: ActorRef): Boolean = relations(other) >= FriendshipPoint
}
