package com.jcp.simscala.environment

import akka.actor.{ActorRef, ActorSystem}
import com.jcp.simscala.environment.EnvironmentCommands.RunCommand
import com.jcp.simscala.event.Event

object DiscreteEnvironment {
  def apply(initialEvent: Event)(implicit AS: ActorSystem): DiscreteEnvironment =
    new DiscreteEnvironment(AS.actorOf(DiscreteEnvironmentActor.props(initialEvent), "DiscreteEnvironment"))
}

class DiscreteEnvironment(actor: ActorRef) extends Environment {
  def run(until: Option[StopCondition] = None): Unit = actor ! RunCommand(until)

  def rewind(to: StopCondition): Event = ???

  override def pause(): Unit = ???

  override def resume(): Unit = ???
}