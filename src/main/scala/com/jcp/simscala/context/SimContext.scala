package com.jcp.simscala.context

import akka.actor.ActorSystem
import com.jcp.simscala.event.{
  Condition,
  Event,
  EventFactory,
  Process,
  ResourceAcquired,
  ResourceRelease,
  ResourceRequest
}
import com.jcp.simscala.resource.ResourceAccess.AccessResponse
import com.jcp.simscala.resource.{ Resource, ResourceAccess }
import com.jcp.simscala.util.TimeHelpers
import com.markatta.timeforscala.Instant
import monocle.macros.GenLens

case class SimContext(time: SimTime,
                      processStack: Seq[Process],
                      conditions: Seq[Condition],
                      resources: Map[Resource, ResourceAccess[_]],
                      triggeredEvents: Set[Event]) {
  override def toString: String =
    s"""[Simulation context @${TimeHelpers.durationFromEpoch(time.now)}:
       | Processes stack: $processStackToString
       | Resources: $resourcesToString]""".stripMargin

  private def processStackToString = processStack.reverse.map(_.name).mkString("->")
  private def resourcesToString    = if (resources.isEmpty) "none" else resources.values.map(_.toString).mkString("\n")
}

object SimContext {
  val init                        = SimContext(SimTime.epoch, Nil, Nil, Map(), Set())
  private val contextLens         = GenLens[SimContext]
  private val contextTimeLens     = contextLens(_.time)
  private val processStackLens    = contextLens(_.processStack)
  private val conditionsLens      = contextLens(_.conditions)
  private val triggeredEventsLens = contextLens(_.triggeredEvents)
  private val resourcesLens       = contextLens(_.resources)

  implicit class SimContextOps(simContext: SimContext)(implicit AS: ActorSystem) {
    def stackHead: Process                              = simContext.processStack.head
    def withStackTail: SimContext                       = processStackLens.modify(_.tail)(simContext)
    def pushOnStack(process: Process): SimContext       = processStackLens.modify(Seq(process) ++ _)(simContext)
    def withTime(time: Instant): SimContext             = contextTimeLens.modify(t => SimTime(time, t.initialTime))(simContext)
    def now: Instant                                    = contextTimeLens.get(simContext).now
    def withCondition(condition: Condition): SimContext = conditionsLens.modify(_ :+ condition)(simContext)
    def matchingConditions: Seq[Condition] =
      simContext.conditions.filter(c => c.events.forall(simContext.triggeredEvents.contains))
    def withoutCondition(condition: Condition): SimContext =
      conditionsLens.modify(_.filterNot(_ == condition))(simContext)
    def withTriggeredEvent(event: Event): SimContext = triggeredEventsLens.modify(_ + event)(simContext)
    def eventFactory: EventFactory                   = EventFactory(simContext)
    def requestResource[R <: Resource](request: ResourceRequest[R]): ResourceOperationResult[R] =
      resourceOperation[R](request.resource, _.request(request))
    def releaseResource[R <: Resource](release: ResourceRelease[R]): ResourceOperationResult[R] =
      resourceOperation[R](release.resource, _.release(release))
    private def resourceOperation[R <: Resource](
      resource: R,
      operation: ResourceAccess[R] => AccessResponse[R]
    ): ResourceOperationResult[R] = {
      val access                                  = accessForResource(resource).getOrElse(ResourceAccess(resource))
      val AccessResponse(updatedAccess, acquired) = operation(access)
      ResourceOperationResult(resourcesLens.modify(_ + (resource -> updatedAccess))(simContext), acquired)
    }
    private def accessForResource[R <: Resource](resource: R): Option[ResourceAccess[R]] =
      simContext.resources.get(resource).map { case access: ResourceAccess[R] => access }
  }

  case class ResourceOperationResult[R <: Resource](updatedContext: SimContext,
                                                    optionalAcquiredEvent: Option[ResourceAcquired[R]])
}
