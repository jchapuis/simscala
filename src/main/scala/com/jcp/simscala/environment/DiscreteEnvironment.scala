package com.jcp.simscala.environment

import akka.actor.{ActorRef, ActorSystem}
import com.jcp.simscala.environment.EnvironmentCommands.RunCommand
import com.jcp.simscala.event.Event
import com.jcp.simscala.event.Process

object DiscreteEnvironment {
  def apply(initialProcess: Process)(implicit AS: ActorSystem): DiscreteEnvironment =
    new DiscreteEnvironment(AS.actorOf(DiscreteEnvironmentActor.props(initialProcess), "DiscreteEnvironment"))
}

class DiscreteEnvironment(actor: ActorRef) extends Environment {
  def run(until: Option[StopCondition] = None): Unit = actor ! RunCommand(until)

  def rewind(to: StopCondition): Event = ???

  override def pause(): Unit = ???

  override def resume(): Unit = ???
}