package com.jcp.simscala.process

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.LoggingReceive
import com.jcp.simscala.command.SimCommand
import com.jcp.simscala.command.SimCommand.{CallbackCommand, InterruptCommand, StartCommand}

trait ProcessActor[B <: ProcessBehavior] extends Actor with ActorLogging {
  override def receive: Receive = LoggingReceive {
    case command: SimCommand => receiveCommand(command)
  }
  private implicit val system: ActorSystem = context.system
  def behavior: B
  private def receiveCommand(command: SimCommand) = (command, behavior) match  {
    case (StartCommand(simContext), b) => sender() ! b.receiveStart(simContext)
    case (CallbackCommand(callback, value, simContext), b: CallbackBehavior) =>
      sender() ! b.receiveCallback(callback, value, simContext)
    case (CallbackCommand(cb, _, _), _) =>
      log.warning(s"Received callback '$cb' for process '${behavior.name}' which does not support it, consider implementing CallbackBehavior")
    case (InterruptCommand(cause, simContext), b: InterruptBehavior) =>
      sender() ! b.receiveInterrupt(cause, simContext)
    case (InterruptCommand(cause, _), _) =>
      log.warning(s"Received interrupt with cause '$cause' for process '${behavior.name}' which does not support it, consider implementing InterruptBehavior")
  }
}

object ProcessActor {
  def props[B <: ProcessBehavior](processBehavior: B): Props = Props(new AnonymousProcessActor(processBehavior))
  private class AnonymousProcessActor[B <: ProcessBehavior](val behavior: B) extends ProcessActor[B]
}