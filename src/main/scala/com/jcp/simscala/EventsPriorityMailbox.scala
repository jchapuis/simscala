package com.jcp.simscala

import akka.actor.ActorSystem
import akka.dispatch.{Envelope, UnboundedStablePriorityMailbox}
import com.jcp.simscala.environment.EnvironmentCommands.EnvironmentCommand
import com.jcp.simscala.event.Event
import com.typesafe.config.Config

class EventsPriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedStablePriorityMailbox(
    (o1: Envelope, o2: Envelope) =>
      (o1.message, o2.message) match {
        case (evt1: Event, evt2: Event)                => evt1.time.compareTo(evt2.time)
        case (command: EnvironmentCommand, other: Any) => 1 // commands are priority
        case (other: Any, command: EnvironmentCommand) => -1
        case (_, _)                                    => 0 // rely on FIFO
    }
  )
