package com.jcp.simscala
import Context._
import Events._
import akka.actor.ActorSystem

object Commands {
  sealed trait SimCommand {
    def simContext: SimContext
  }

  case class StartCommand(simContext: SimContext)(implicit AS: ActorSystem) extends SimCommand
  case class CallbackCommand[T](message: CallbackMessage, value: T, simContext: SimContext)(implicit AS: ActorSystem)
    extends SimCommand

  sealed trait FailureCommand                                                                  extends SimCommand
  case class InterruptCommand(cause: String, simContext: SimContext)(implicit AS: ActorSystem) extends FailureCommand
}
