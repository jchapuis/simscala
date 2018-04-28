package com.jcp.simscala

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import Commands._
import Context._
import Events._
import akka.event.LoggingReceive
import com.typesafe.scalalogging.Logger

trait ProcessActor extends Actor with ActorLogging {
  override def receive: Receive = LoggingReceive {
    case command: SimCommand => receiveCommand(command)
  }
  private implicit val system: ActorSystem = context.system
  def behavior: ProcessBehavior
  def receiveCommand(command: SimCommand): Unit = command match {
    case StartCommand(simContext) => sender() ! behavior.receiveStart(simContext, command.eventFactory)
    case CallbackCommand(callback, value, simContext) =>
      sender() ! behavior.receiveContinue(callback, value, simContext, command.eventFactory)
    case InterruptCommand(cause, simContext) =>
      sender() ! behavior.receiveInterrupt(cause, simContext, command.eventFactory)
  }
}

trait ProcessBehavior {
  def receiveStart(simContext: SimContext, eventFactory: EventFactory): Event
  def receiveContinue[T](callback: Any, value: T, simContext: SimContext, eventFactory: EventFactory): Event
  def receiveInterrupt(cause: String, simContext: SimContext, eventFactory: EventFactory): Event
  def name: EventName
  def logger: Logger = Logger(s"Process:$name")
}

object ProcessActor {
  def props(processBehavior: ProcessBehavior): Props = Props(new AnonymousProcessActor(processBehavior))
  private class AnonymousProcessActor(val behavior: ProcessBehavior) extends ProcessActor
}
