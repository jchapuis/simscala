package com.jcp.simscala.event

import akka.actor.ActorRef
import com.jcp.simscala.event.Event.{CallbackMessage, EventName}
import com.markatta.timeforscala.Instant
import scalaz.Show

case class Process(processActor: ActorRef,
                   name: EventName,
                   time: Instant,
                   callbackMessageOnEnd: CallbackMessage,
                   parent: Option[Process])
  extends Event {
  def endEventName = s"endOf($name)"
}

object Process {
  implicit val show = Show.shows[Process](_.name)
}
