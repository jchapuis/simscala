package com.jcp.simscala.event

import akka.actor.{ ActorSystem, Props }
import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Event.{ CallbackMessage, EventName }
import com.jcp.simscala.process.{ ProcessActor, ProcessBehavior }
import com.jcp.simscala.resource.Resource

case class EventFactory(simContext: SimContext)(implicit AS: ActorSystem) {
  private def now            = simContext.time.now
  private def currentProcess = simContext.processStack.head

  def process(props: Props, name: EventName, callbackMessage: CallbackMessage): Process =
    Process(AS.actorOf(props, name), name, now, callbackMessage)
  def process(processBehavior: ProcessBehavior, callbackMessage: CallbackMessage): Process =
    Process(
      AS.actorOf(ProcessActor.props(processBehavior), processBehavior.name),
      processBehavior.name,
      now,
      callbackMessage
    )
  def delayedCallback[T](delay: com.markatta.timeforscala.Duration, callbackMessage: CallbackMessage, value: T) =
    DelayedCallbackEvent[T](delay, now, currentProcess, callbackMessage, value)
  def delayedCallback(delay: com.markatta.timeforscala.Duration, callbackMessage: CallbackMessage) =
    DelayedCallbackEvent[Unit](delay, now, currentProcess, callbackMessage, ())
  def processEnd[T](value: T)                = ProcessEnd(simContext.stackHead, now, value)
  def conditionMatched(condition: Condition) = ConditionMatchedEvent(condition, now)
  def allOf[T](events: List[Event], callbackMessage: Any) =
    AllOf(events, now, currentProcess, callbackMessage)
  def anyOf[T](events: List[Event], callbackMessage: Any) =
    AnyOf(events, now, currentProcess, callbackMessage)
  def requestResource[R <: Resource](resource: R) = ResourceRequest(resource, currentProcess, now)
  def releaseResource[R <: Resource](resource: R) = ResourceRelease(resource, currentProcess, now)
  def never = Event.Never
}

object EventFactory {
  def initialProcess(processBehavior: ProcessBehavior)(implicit AS: ActorSystem): Process =
    SimContext.init.eventFactory.process(processBehavior, callbackMessage = None)
}
