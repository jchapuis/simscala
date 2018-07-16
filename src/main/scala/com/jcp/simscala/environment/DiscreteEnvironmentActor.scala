package com.jcp.simscala.environment

import akka.actor.{ Actor, ActorLogging, ActorSystem, PoisonPill, Props }
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.jcp.simscala.command.SimCommand.{ CallbackCommand, ResourceAcquiredCommand, StartCommand }
import com.jcp.simscala.context.SimContext
import com.jcp.simscala.context.SimContext.ResourceOperationResult
import com.jcp.simscala.environment.EnvironmentCommands._
import com.jcp.simscala.event.{ Event, _ }
import com.jcp.simscala.resource.Resource
import com.jcp.simscala.util.TimeHelpers

import scala.concurrent.duration._
import scala.language.postfixOps
object DiscreteEnvironmentActor {
  def props(initialEvent: Event)(implicit AS: ActorSystem): Props =
    Props(new DiscreteEnvironmentActor(initialEvent)).withMailbox("events-priority-mailbox")
}

class DiscreteEnvironmentActor(initialEvent: Event)(implicit AS: ActorSystem) extends Actor with ActorLogging {
  import AS.dispatcher
  implicit val timeout: Timeout = Timeout(5 seconds) // needed for `?` below
  var simContext                = SimContext.init

  override def receive: Receive = {
    case command: EnvironmentCommand => receiveCommand(command)
    case event: CompositeEvent =>
      event.events.foreach(self ! _)
    case event: Event if event != Event.Never =>
      logDebug(s"Received event ${event.name}: ${simContext}")
      simContext.matchingConditions.foreach(c => receiveEvent(simContext.eventFactory.conditionMatched(c)))
      receiveEvent(event)
  }

  private def receiveCommand(command: EnvironmentCommand): Unit = command match {
    case RunCommand(None) => // todo implement stop conditions
      receiveEvent(initialEvent)
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
        simContext = simContext.withTime(callback.time)
        callback.callbackProcess.processActor ? CallbackCommand(
          callback.callbackMessage,
          callback.value,
          simContext
        ) pipeTo self

      case ConditionMatchedEvent(condition, _) =>
        logDebug(s"Condition matched: '$condition'")
        simContext = simContext.withoutCondition(condition)
        condition.callbackProcess.processActor ? condition.callbackMessage pipeTo self

      case newProcess @ Process(actorRef, name, _, _) =>
        logDebug(s"Starting process with name '$name'")
        simContext = simContext.pushOnStack(newProcess) // todo this is wrong, stack is per process since with delayed callback we can have other things executing of course
        actorRef ? StartCommand(simContext) pipeTo self

      case ProcessEnd(_, _, value) if simContext.processStack.tail.isEmpty =>
        logDebug(s"Simulation ended with value '$value'")
        self ! PoisonPill
        context.system.terminate()

      case ProcessEnd(Process(_, name, _, callbackMessage), _, value) =>
        logDebug(s"Process with name '$name' ended with value '$value'")
        sender() ! PoisonPill
        simContext = simContext.withStackTail
        val parentProcess = simContext.stackHead.processActor
        parentProcess ? CallbackCommand(callbackMessage, value, simContext) pipeTo self

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

  private def resourceOperation[R <: Resource](operation: => ResourceOperationResult[R]) = {
    val result = operation
    simContext = result.updatedContext
    result.optionalAcquiredEvent.foreach(
      acquired => acquired.process.processActor ? ResourceAcquiredCommand(acquired.resource, simContext) pipeTo self
    )
  }

  private def receiveCondition(condition: Condition): Unit = {
    logDebug(s"Adding condition '${condition.name}'")
    simContext = simContext.withCondition(condition)
    condition.events.foreach(e => self ! e)
  }

  private def logDebug(message: => String) = log.debug(s"@${TimeHelpers.durationFromEpoch(simContext.now)} - $message")
}
