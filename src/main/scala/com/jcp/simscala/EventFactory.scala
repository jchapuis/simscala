package com.jcp.simscala
import akka.actor.{ ActorSystem, Props }
import com.jcp.simscala.Context.SimContext
import com.jcp.simscala.Events._
import com.markatta.timeforscala._

case class EventFactory(simContext: SimContext)(implicit AS: ActorSystem) {
  def process(props: Props, name: EventName, callbackMessage: CallbackMessage): ProcessStart =
    ProcessStart(AS.actorOf(props), name, simContext, callbackMessage)
  def process(processBehavior: ProcessBehavior, name: EventName, callbackMessage: CallbackMessage): ProcessStart =
    ProcessStart(AS.actorOf(ProcessActor.props(processBehavior)), name, simContext, callbackMessage)
  def timeout[T](delay: com.markatta.timeforscala.Duration,
                 callbackMessage: CallbackMessage,
                 value: T): TimeoutEvent[T] =
    TimeoutEvent[T](delay, simContext.time.now + delay, simContext, callbackMessage, value)
  def processEnd[T](value: T) = ProcessEnd(simContext.stackHead, simContext, value)
}

object EventFactory {
  def rootProcess(processBehavior: ProcessBehavior)(implicit AS: ActorSystem): ProcessStart =
    ProcessStart(AS.actorOf(ProcessActor.props(processBehavior)), "root", SimContext.init, "")
}
