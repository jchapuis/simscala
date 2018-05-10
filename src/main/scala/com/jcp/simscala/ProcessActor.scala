package com.jcp.simscala

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import Commands._
import Context._
import Events._
import akka.event.LoggingReceive
import com.typesafe.scalalogging.Logger

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

trait StartBehavior {
  def receiveStart(simContext: SimContext): Event
}

trait CallbackBehavior {
  def receiveCallback[T](callback: Any, value: T, simContext: SimContext): Event
}

trait InterruptBehavior {
  def receiveInterrupt(cause: String, simContext: SimContext): Event
}

trait ProcessBehavior extends StartBehavior {
  def name: EventName
  def logger: Logger = Logger(s"Process:$name")
}

object ProcessActor {
  def props[B <: ProcessBehavior](processBehavior: B): Props = Props(new AnonymousProcessActor(processBehavior))
  private class AnonymousProcessActor[B <: ProcessBehavior](val behavior: B) extends ProcessActor[B]
}
