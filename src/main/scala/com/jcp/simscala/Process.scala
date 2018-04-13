package com.jcp.simscala

import akka.actor.{ Actor, ActorLogging }
import Commands._
import Context._
import Events._

trait ProcessActor extends Actor with ActorLogging {

  def receiveStart(simContext: SimContext): Event
  def receiveContinue(callback: Any, simContext: SimContext): Event

  override def receive: Receive = {
    case StartCommand(simContext)              => sender() ! receiveStart(simContext)
    case ContinueCommand(callback, simContext) => sender() ! receiveContinue(callback, simContext)
  }
}
