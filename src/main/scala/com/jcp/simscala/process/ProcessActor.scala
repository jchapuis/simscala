package com.jcp.simscala.process

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.LoggingReceive
import com.jcp.simscala.command.SimCommand
import com.jcp.simscala.command.SimCommand._
import com.jcp.simscala.event.Event

trait ProcessActor[B <: ProcessBehavior] extends Actor with ActorLogging {
  override def receive: Receive = LoggingReceive {
    case command: SimCommand => receiveCommand(command)
  }
  private implicit val system: ActorSystem = context.system
  def behavior: B
  private def receiveCommand(command: SimCommand) = (command, behavior) match {
    case (StartCommand(simContext), b) => sender() ! b.start(simContext)
    case (CallbackCommand(callback, value, simContext), b: CallbackBehavior) =>
      sender() ! b.receiveCallback(callback, value)(simContext)
    case (CallbackCommand(cb, _, _), _) =>
      log.warning(
        s"Received callback '$cb' for process '${behavior.name}' which does not support it, consider implementing CallbackBehavior"
      )
      sender() ! Event.Never
    case (ResourceAcquiredCommand(resource, simContext), b: ResourceBehavior) =>
      sender() ! b.resourceAcquired(resource)(simContext)
    case (ResourceAcquiredCommand(resource, _), _) =>
      log.warning(
        s"Received resource acquired command (resource ${resource.name}) for process '${behavior.name}' which does not support it, consider implementing ResourceBehavior"
      )
      sender() ! Event.Never
    case (ConditionMatchedCommand(condition, simContext), b: ConditionBehavior) =>
      sender() ! b.receiveConditionMatched(condition)(simContext)
    case (ConditionMatchedCommand(condition, simContext), _) =>
      log.warning(
        s"Received condition matched command (resource ${condition.name}) for process '${behavior.name}' which does not support it, consider implementing ConditionBehavior"
      )
      sender() ! Event.Never
    case (InterruptCommand(cause, simContext), b: InterruptBehavior) =>
      sender() ! b.interrupted(cause)(simContext)
    case (InterruptCommand(cause, _), _) =>
      log.warning(
        s"Received interrupt with cause '$cause' for process '${behavior.name}' which does not support it, consider implementing InterruptBehavior"
      )
      sender() ! Event.Never
  }
}

object ProcessActor {
  def props[B <: ProcessBehavior](processBehavior: B): Props = Props(new BehaviorBasedProcessActor(processBehavior))
  private class BehaviorBasedProcessActor[B <: ProcessBehavior](val behavior: B) extends ProcessActor[B]
}
