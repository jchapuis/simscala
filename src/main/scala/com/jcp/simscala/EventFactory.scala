package com.jcp.simscala
import akka.actor.{ ActorSystem, Props }
import com.jcp.simscala.Context.SimContext
import com.jcp.simscala.Events._
import com.markatta.timeforscala._

case class EventFactory(simContext: SimContext)(implicit AS: ActorSystem) {
  private def now            = simContext.time.now
  private def currentProcess = simContext.processStack.head

  def process(props: Props, name: EventName, callbackMessage: CallbackMessage): Process =
    Process(AS.actorOf(props), name, now, callbackMessage)
  def process(processBehavior: ProcessBehavior, callbackMessage: CallbackMessage): Process =
    Process(
      AS.actorOf(ProcessActor.props(processBehavior)),
      processBehavior.name,
      now,
      callbackMessage
    )

  def delayedCallback[T](delay: com.markatta.timeforscala.Duration,
                         callbackMessage: CallbackMessage,
                         value: T): DelayedCallbackEvent[T] =
    DelayedCallbackEvent[T](delay, now, currentProcess, callbackMessage, value)
  def delayedCallback(delay: com.markatta.timeforscala.Duration,
                      callbackMessage: CallbackMessage): DelayedCallbackEvent[Unit] =
    DelayedCallbackEvent[Unit](delay, now, currentProcess, callbackMessage, ())
  def processEnd[T](value: T)                = ProcessEnd(simContext.stackHead, now, value)
  def conditionMatched(condition: Condition) = ConditionMatchedEvent(condition, now)
  def allOf[T](events: Seq[Event], callbackProcess: Process, callbackMessage: Any) =
    AllOf(events, now, callbackProcess, callbackMessage)
  def anyOf[T](events: Seq[Event], callbackProcess: Process, callbackMessage: Any) =
    AnyOf(events, now, callbackProcess, callbackMessage)
}

object EventFactory {
  def initialProcess(processBehavior: ProcessBehavior)(implicit AS: ActorSystem): Process =
    SimContext.init.eventFactory.process(processBehavior, callbackMessage = None)
}
