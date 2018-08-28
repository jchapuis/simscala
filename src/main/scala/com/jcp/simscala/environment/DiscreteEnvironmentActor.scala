package com.jcp.simscala.environment

import akka.actor.{ Actor, ActorLogging, ActorSystem, PoisonPill, Props }
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.jcp.simscala.command.SimCommand.{
  CallbackCommand,
  ConditionMatchedCommand,
  ResourceAcquiredCommand,
  StartCommand
}
import com.jcp.simscala.context.SimContext
import com.jcp.simscala.context.SimContext.ResourceOperationResult
import com.jcp.simscala.environment.EnvironmentCommands._
import com.jcp.simscala.event.{ Event, _ }
import com.jcp.simscala.resource.Resource
import com.jcp.simscala.util.TimeHelpers

import scala.concurrent.duration._
import scala.language.postfixOps
object DiscreteEnvironmentActor {
  def props(initialProcess: Process)(implicit AS: ActorSystem): Props =
    Props(new DiscreteEnvironmentActor(initialProcess)).withMailbox("events-priority-mailbox")
}

class DiscreteEnvironmentActor(rootProcess: Process)(implicit AS: ActorSystem) extends Actor with ActorLogging {
  import AS.dispatcher
  implicit val timeout: Timeout = Timeout(30 seconds) // needed for `?` below
  var simContext                = SimContext(rootProcess)

  override def receive: Receive = {
    case command: EnvironmentCommand =>
      logDebug(s"Received environment command ${command}")
      receiveCommand(command)
    case event: CompositeEvent =>
      logDebug(s"Received composite event (${event.events.map(_.name).mkString(" ,")}): $simContext")
      event.events.foreach(self ? _ pipeTo self)
    case event: Event if event != Event.Never =>
      logDebug(s"Received event ${event.name}: $simContext")
      receiveEvent(event)
      simContext.matchingConditions.foreach(c => receiveEvent(EventFactory.conditionMatched(c)(simContext)))
  }

  private def receiveCommand(command: EnvironmentCommand): Unit = command match {
    case RunCommand(None) => // todo implement stop conditions
      receiveEvent(rootProcess)
    case RunCommand(Some(stopCondition)) => ???
    case PauseCommand                    => ???
    case RewindCommand(to)               => ???
    case ResumeCommand                   => ???
  }

  private def receiveEvent(event: Event) = {
    simContext = simContext.withTriggeredEvent(event)
    event match {
      case callback: DelayedCallbackEvent[_] =>
        logDebug(
          s"Delayed callback triggered: process = '${callback.callbackProcess.name}', elapsed delay = '${callback.delay}', name = '${callback.name}'"
        )
        simContext = simContext.withCurrentProcess(callback.callbackProcess).withTime(callback.time)
        callback.callbackProcess.processActor ? CallbackCommand(
          callback.callbackMessage,
          callback.value,
          simContext
        ) pipeTo self

      case ConditionMatchedEvent(condition, _) =>
        logDebug(s"Condition matched: '${condition.name}'")
        simContext = simContext.withoutCondition(condition)
        setCurrentProcessAndAsk(condition.callbackProcess, ConditionMatchedCommand(condition, _))

      case rootProcess @ Process(_, name, _, _, None) =>
        logDebug(s"Starting root process with name '$name'")
        askCurrentProcess(StartCommand(simContext))

      case newProcess @ Process(_, name, _, _, Some(parentProcess)) =>
        logDebug(s"Starting process with name '$name'")
        simContext = simContext.withCurrentProcess(parentProcess)
        simContext = simContext.pushOnCurrentProcessStack(newProcess)
        askCurrentProcess(StartCommand(simContext))

      case ProcessEnd(process, _, value) if process.parent.isEmpty =>
        logDebug(s"Simulation ended with value '$value'")
        process.processActor ! PoisonPill
        context.system.terminate()

      case ProcessEnd(process @ Process(_, name, _, callbackMessage, _), _, value) =>
        logDebug(s"Process with name '$name' ended with value '$value'")
        simContext = simContext.withCurrentProcess(process)
        simContext = simContext.popCurrentProcessStack
        process.processActor ! PoisonPill
        askCurrentProcess(CallbackCommand(callbackMessage, value, simContext))

      case allOf: AllOf => receiveCondition(allOf)
      case anyOf: AnyOf => receiveCondition(anyOf)

      case request: ResourceRequest[_] =>
        logDebug(s"Request for resource '${request.resource.name}' coming from process '${request.process.name}'")
        resourceOperation(simContext.requestResource(request))

      case release: ResourceRelease[_] =>
        logDebug(s"Resource'${release.resource.name}' released by process '${release.process.name}'")
        resourceOperation(simContext.releaseResource(release))
    }
  }

  private def setCurrentProcessAndAsk[T](process: Process, message: SimContext => T): Unit = {
    simContext = simContext.withCurrentProcess(process)
    process.processActor ? message(simContext) pipeTo self
  }

  private def askCurrentProcess[T](message: T) = simContext.currentProcess.processActor ? message pipeTo self

  private def resourceOperation[R <: Resource](operation: => ResourceOperationResult[R]) = {
    val result = operation
    simContext = result.updatedContext
    result.optionalAcquiredEvent.foreach(
      acquired => {
        setCurrentProcessAndAsk(acquired.process, ResourceAcquiredCommand(acquired.resource, _))
      }
    )
  }

  private def receiveCondition(condition: Condition): Unit = {
    logDebug(s"Adding condition '${condition.name}'")
    simContext = simContext.withCondition(condition)
  }

  private def logDebug(message: => String) = log.debug(s"@${TimeHelpers.durationFromEpoch(simContext.now)} - $message")
}
