package com.jcp.simscala

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.jcp.simscala.Commands._
import com.jcp.simscala.Context.SimContext
import com.jcp.simscala.EnvironmentCommands.RunCommand
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
  case class RunCommand(until: Option[StopCondition])
  case object PauseCommand
  case object ResumeCommand
  case class RewindCommand(to: StopCondition)
}

class DiscreteEnvironment(actor: ActorRef) extends Environment {
  def run(until: Option[StopCondition] = None): Unit = actor ! RunCommand(until)

  def rewind(to: StopCondition): Event = ???

  // TODO allOf, anyOf
  override def pause(): Unit = ???

  override def resume(): Unit = ???
}

object DiscreteEnvironment {
  def apply(rootProcess: ProcessStart)(implicit AS: ActorSystem): DiscreteEnvironment =
    new DiscreteEnvironment(AS.actorOf(DiscreteEnvironmentActor.props(rootProcess), "simMaster"))
}

object DiscreteEnvironmentActor {
  def props(rootProcess: ProcessStart)(implicit AS: ActorSystem): Props =
    Props(new DiscreteEnvironmentActor(rootProcess))
}

class DiscreteEnvironmentActor(rootProcess: ProcessStart)(implicit AS: ActorSystem) extends Actor with ActorLogging {
  implicit val timeout: Timeout = Timeout(5 seconds) // needed for `?` below
  import scala.concurrent.ExecutionContext.Implicits.global

  sealed trait SimStep
  case object JumpStep                                                    extends SimStep
  case class ProcessStep(process: Process, parentStack: Seq[ProcessStep]) extends SimStep

  override def receive: Receive = {
    case RunCommand(until) =>
      rootProcess.processActor ? StartCommand(
        SimContext.init.withTime(Instant.now).pushOnStack(rootProcess)
      ) pipeTo self

    case newTimeout: TimeoutEvent[_] =>
      log.info(
        s"${newTimeout.simContext.now} - Enqueuing new timeout: parent process = ${newTimeout.simContext.stackHead.name}, delay = ${newTimeout.delay}, name = ${newTimeout.name}"
      )
      val (nextTimeout, nextContext) = newTimeout.simContext.enqueueTimeout(newTimeout).dequeueNextTimeout()
      val parentProcess              = nextTimeout.simContext.stackHead
      parentProcess.processActor ? CallbackCommand(
        nextTimeout.callbackMessage,
        nextTimeout.value,
        nextContext
      ) pipeTo self

    case newProcess @ ProcessStart(actorRef, name, simContext, _) =>
      log.info(s"${simContext.now} - Starting process with name $name")
      actorRef ? StartCommand(simContext.pushOnStack(newProcess)) pipeTo self

    case ProcessEnd(p, simContext, value) if p == rootProcess =>
      log.info(s"${simContext.now} - Simulation ended with value $value")
      self ! PoisonPill
      context.system.terminate()

    case ProcessEnd(ProcessStart(_, name, _, callbackMessage), simContext, value) =>
      log.info(s"${simContext.now} - Process with name $name ended with value $value")
      val parentProcess = simContext.withStackTail.stackHead.processActor
      parentProcess ? CallbackCommand(callbackMessage, value, simContext.withStackTail) pipeTo self
  }

}
