package com.jcp.simscala

import akka.actor.ActorRef
import Context._
import com.markatta.timeforscala.{Duration, Instant}

object Events {
  sealed trait Event {
    def context: SimContext
  }
  case class Process(actorRef: ActorRef, context: SimContext)                 extends Event
  case class DelayedEvent(event: Event, delay: Duration, context: SimContext) extends Event
  case class EndEvent(sender: ActorRef, context: SimContext)                  extends Event

  implicit class EventOps(event: Event) {
    def withTime(simTime: Instant): Event = {
      event match {
        case process : Process => process.copy(context = process.context.withTime(simTime))
        case delayedEvent : DelayedEvent => delayedEvent.copy(context = delayedEvent.context.withTime(simTime))
        case end: EndEvent => end.copy(context = end.context.withTime(simTime))
      }
    }
  }
}
