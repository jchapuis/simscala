package com.jcp.simscala.event

import akka.actor.ActorRef
import com.jcp.simscala.event.Event.{CallbackMessage, EventName}
import com.markatta.timeforscala.Instant

case class Process(processActor: ActorRef,
                   name: EventName,
                   time: Instant,
                   callbackMessageOnEnd: CallbackMessage)
  extends Event
