package com.jcp.simscala.command

import akka.actor.ActorSystem
import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Condition
import com.jcp.simscala.event.Event.CallbackMessage
import com.jcp.simscala.resource.Resource

sealed trait SimCommand {
  def simContext: SimContext
}

case object SimCommand {
  case class StartCommand(simContext: SimContext)(implicit AS: ActorSystem)                    extends SimCommand
  case class InterruptCommand(cause: String, simContext: SimContext)(implicit AS: ActorSystem) extends FailureCommand
  sealed trait FailureCommand                                                                  extends SimCommand
  case class CallbackCommand[T](message: CallbackMessage, value: T, simContext: SimContext)(implicit AS: ActorSystem)
    extends SimCommand
  case class ResourceAcquiredCommand[R <: Resource](resource: R, simContext: SimContext)(implicit AS: ActorSystem)
    extends SimCommand
  case class ConditionMatchedCommand(condition: Condition, simContext: SimContext)(implicit AS: ActorSystem)
    extends SimCommand
}
