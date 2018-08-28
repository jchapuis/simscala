package com.jcp.simscala.event

import akka.actor.{ ActorSystem, Props }
import com.jcp.simscala.context.{ SimContext, SimTime }
import com.jcp.simscala.context.SimContext._
import com.jcp.simscala.event.Event.{ CallbackMessage, EventName }
import com.jcp.simscala.process.{ ProcessActor, ProcessBehavior }
import com.jcp.simscala.resource.Resource
import com.markatta.timeforscala.Instant

trait EventFactory {
  private def now(implicit SC: SimContext) = SC.time.now

  def process(props: Props, name: EventName, callbackMessage: CallbackMessage)(implicit SC: SimContext,
                                                                               AS: ActorSystem): Process =
    Process(AS.actorOf(props, name), name, now, callbackMessage, Some(SC.currentProcess))
  def process(processBehavior: ProcessBehavior, callbackMessage: CallbackMessage)(implicit SC: SimContext,
                                                                                  AS: ActorSystem): Process =
    process(
      ProcessActor.props(processBehavior),
      processBehavior.name,
      callbackMessage
    )
  def process(processBehavior: ProcessBehavior)(implicit SC: SimContext, AS: ActorSystem): Process =
    process(processBehavior, None)
  def rootProcess(props: Props, name: EventName, callbackMessage: CallbackMessage)(implicit
                                                                                   AS: ActorSystem): Process =
    Process(AS.actorOf(props, name), name, Instant.Epoch, callbackMessage, None)
  def rootProcess(processBehavior: ProcessBehavior, callbackMessage: CallbackMessage)(implicit
                                                                                      AS: ActorSystem): Process =
    rootProcess(
      ProcessActor.props(processBehavior),
      processBehavior.name,
      callbackMessage
    )
  def rootProcess(processBehavior: ProcessBehavior)(implicit AS: ActorSystem): Process =
    rootProcess(processBehavior, None)
  def delayedCallback[T](delay: com.markatta.timeforscala.Duration, callbackMessage: CallbackMessage, value: T)(
    implicit SC: SimContext
  ) =
    DelayedCallbackEvent[T](delay, now, SC.currentProcess, callbackMessage, value)
  def delayedCallback(delay: com.markatta.timeforscala.Duration,
                      callbackMessage: CallbackMessage)(implicit SC: SimContext) =
    DelayedCallbackEvent[Unit](delay, now, SC.currentProcess, callbackMessage, ())
  def processEnd[T](value: T)(implicit SC: SimContext)                = ProcessEnd(SC.currentProcess, now, value)
  def processEnd(implicit SC: SimContext): ProcessEnd[None.type]      = processEnd(None)
  def conditionMatched(condition: Condition)(implicit SC: SimContext) = ConditionMatchedEvent(condition, now)
  def allOf(events: List[EventName], callbackMessage: Any)(implicit SC: SimContext) =
    AllOf(events, now, SC.currentProcess, callbackMessage)
  def anyOf(events: List[EventName], callbackMessage: Any)(implicit SC: SimContext) =
    AnyOf(events, now, SC.currentProcess, callbackMessage)
  def requestResource[R <: Resource](resource: R)(implicit SC: SimContext) =
    ResourceRequest(resource, SC.currentProcess, now)
  def releaseResource[R <: Resource](resource: R)(implicit SC: SimContext) =
    ResourceRelease(resource, SC.currentProcess, now)
  def never = Event.Never
}

case object EventFactory extends EventFactory {}
