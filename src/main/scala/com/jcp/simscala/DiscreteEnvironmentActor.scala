package com.jcp.simscala

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.jcp.simscala.Commands._
import com.jcp.simscala.Context.SimContext
import com.jcp.simscala.EnvironmentCommands._
import com.jcp.simscala.Events._

import scala.concurrent.duration._
import scala.language.postfixOps

trait Environment {
  def run(until: Option[StopCondition])
  def rewind(to: StopCondition): Event
  def pause()
  def resume()
}

sealed trait StopCondition
case object NoMoreEvents                            extends StopCondition
case class SomeEventTriggered(eventName: EventName) extends StopCondition
case class SomeInstantReached(instant: Instant)     extends StopCondition

object EnvironmentCommands {
  sealed trait EnvironmentCommand
  case class RunCommand(until: Option[StopCondition]) extends EnvironmentCommand
  case object PauseCommand                            extends EnvironmentCommand
  case object ResumeCommand                           extends EnvironmentCommand
  case class RewindCommand(to: StopCondition)         extends EnvironmentCommand
}

class DiscreteEnvironment(actor: ActorRef) extends Environment {
  def run(until: Option[StopCondition] = None): Unit = actor ! RunCommand(until)

  def rewind(to: StopCondition): Event = ???

  // TODO allOf, anyOf
  override def pause(): Unit = ???

  override def resume(): Unit = ???
}

object DiscreteEnvironment {
  def apply(initialEvent: Event)(implicit AS: ActorSystem): DiscreteEnvironment =
    new DiscreteEnvironment(AS.actorOf(DiscreteEnvironmentActor.props(initialEvent), "DiscreteEnvironment"))
}

object DiscreteEnvironmentActor {
  def props(initialEvent: Event)(implicit AS: ActorSystem): Props =
    Props(new DiscreteEnvironmentActor(initialEvent)).withMailbox("events-priority-mailbox")
}

class DiscreteEnvironmentActor(initialEvent: Event)(implicit AS: ActorSystem) extends Actor with ActorLogging {
  implicit val timeout: Timeout = Timeout(5 seconds) // needed for `?` below
  import scala.concurrent.ExecutionContext.Implicits.global
  var simContext = SimContext.init

  override def receive: Receive = {
    case command: EnvironmentCommand => receiveCommand(command)
    case event: Event =>
      logDebug(s"Received event ${event.name}")
      simContext.matchingConditions.foreach(c => receiveEvent(simContext.eventFactory.conditionMatched(c)))
      receiveEvent(event)
  }

  private def receiveCommand(command: EnvironmentCommand): Unit = command match {
    case RunCommand(None) => // todo implement stop conditions
      receiveEvent(initialEvent)
    case RunCommand(Some(stopCondition)) => ???
    case PauseCommand => ???
    case RewindCommand(to) => ???
    case ResumeCommand => ???
  }

  private def receiveEvent(event: Event) = {
    simContext = simContext.withTriggeredEvent(event)
    event match {
      case callback: DelayedCallbackEvent[_] =>
        logDebug(
          s"Delayed callback timed out: process = ${callback.callbackProcess.name}, elapsed delay = ${callback.delay}, name = ${callback.name}"
        )
        simContext = simContext.withTime(callback.time)
        callback.callbackProcess.processActor ? CallbackCommand(
          callback.callbackMessage,
          callback.value,
          simContext
        ) pipeTo self

      case ConditionMatchedEvent(condition, _) =>
        logDebug(s"Condition matched: $condition")
        simContext = simContext.withoutCondition(condition)
        condition.callbackProcess.processActor ? condition.callbackMessage pipeTo self

      case newProcess @ Process(actorRef, name, _, _) =>
        logDebug(s"Starting process with name $name")
        simContext = simContext.pushOnStack(newProcess)
        actorRef ? StartCommand(simContext) pipeTo self

      case ProcessEnd(_, _, value) if simContext.processStack.tail.isEmpty =>
        logDebug(s"Simulation ended with value $value")
        self ! PoisonPill
        context.system.terminate()

      case ProcessEnd(Process(_, name, _, callbackMessage), _, value) =>
        logDebug(s"Process with name $name ended with value $value")
        simContext = simContext.withStackTail
        val parentProcess = simContext.stackHead.processActor
        parentProcess ? CallbackCommand(callbackMessage, value, simContext) pipeTo self

      case allOf: AllOf => receiveCondition(allOf)
      case anyOf: AnyOf => receiveCondition(anyOf)
    }
  }

  private def receiveCondition(condition: Condition): Unit = {
    logDebug(s"Adding condition ${condition.name}")
    simContext = simContext.withCondition(condition)
    condition.events.foreach(e => self ! e)
  }

  private def logDebug(message: => String) = log.debug(s"${simContext.now} - $message")
}
