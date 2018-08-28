package com.jcp.simscala.context

import com.jcp.simscala.event.{ Condition, Event, Process, ResourceAcquired, ResourceRelease, ResourceRequest }
import com.jcp.simscala.resource.ResourceAccess.AccessResponse
import com.jcp.simscala.resource.{ Resource, ResourceAccess }
import com.jcp.simscala.util.TimeHelpers
import com.markatta.timeforscala.Instant
import monocle.macros.GenLens
import scalaz.Tree._
import scalaz.{ Tree, TreeLoc }

case class SimContext(time: SimTime,
                      processTree: Tree[Process],
                      currentProcessLoc: TreeLoc[Process],
                      conditions: Seq[Condition],
                      resources: Map[Resource, ResourceAccess[_]],
                      triggeredEvents: Set[Event]) {

  override def toString: String =
    s"""[Simulation context @${TimeHelpers.durationFromEpoch(time.now)}:
       |Resources: $resourcesToString]
       |Processes tree:
       |${processTree.drawTree}""".stripMargin

  private def resourcesToString = if (resources.isEmpty) "none" else resources.values.map(_.toString).mkString("\n")
}

object SimContext {
  def apply(rootProcess: Process): SimContext = {
    val tree = Tree.Leaf(rootProcess)
    SimContext(SimTime.epoch, tree, tree.loc, Nil, Map(), Set())
  }

  private val contextLens           = GenLens[SimContext]
  private val contextTimeLens       = contextLens(_.time)
  private val processTreeLens       = contextLens(_.processTree)
  private val currentProcessLocLens = contextLens(_.currentProcessLoc)
  private val conditionsLens        = contextLens(_.conditions)
  private val triggeredEventsLens   = contextLens(_.triggeredEvents)
  private val resourcesLens         = contextLens(_.resources)

  implicit class SimContextOps(simContext: SimContext) {
    def isCurrentProcessRootProcess: Boolean = simContext.currentProcessLoc.isRoot
    def currentProcess: Process              = simContext.currentProcessLoc.getLabel
    def withCurrentProcess(process: Process): SimContext =
      if (currentProcess == process) simContext
      else {
        val loc = simContext.processTree.loc.root.find(_.getLabel == process)
        require(loc.isDefined, "current process must be part of tree")
        currentProcessLocLens.set(loc.get)(simContext)
      }
    def popCurrentProcessStack: SimContext = {
      assert(!isCurrentProcessRootProcess, "trying to pop root process")
      (currentProcessLocLens.modify(_.parent.get) compose processTreeLens.modify(
        _ => simContext.currentProcessLoc.delete.get.toTree
      ))(simContext)
    }
    def pushOnCurrentProcessStack(process: Process): SimContext = {
      assert(currentProcess != process, "trying to push process onto itself")
      SimContextOps(
        currentProcessLocLens
          .modify(_.insertDownLast(Leaf(process)))(simContext)
      ).commitTree
    }
    def withTime(time: Instant): SimContext             = contextTimeLens.modify(t => SimTime(time, t.initialTime))(simContext)
    def now: Instant                                    = contextTimeLens.get(simContext).now
    def withCondition(condition: Condition): SimContext = conditionsLens.modify(_ :+ condition)(simContext)
    def matchingConditions: Seq[Condition] = {
      val triggeredEventNames = simContext.triggeredEvents.map(_.name)
      simContext.conditions.filter(c => c.events.forall(triggeredEventNames.contains))
    }
    def withoutCondition(condition: Condition): SimContext =
      conditionsLens.modify(_.filterNot(_ == condition))(simContext)
    def withTriggeredEvent(event: Event): SimContext = triggeredEventsLens.modify(_ + event)(simContext)
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
      simContext.resources.get(resource).map(_.asInstanceOf[ResourceAccess[R]])
    private def commitTree = processTreeLens.set(simContext.currentProcessLoc.toTree)(simContext)
  }

  case class ResourceOperationResult[R <: Resource](updatedContext: SimContext,
                                                    optionalAcquiredEvent: Option[ResourceAcquired[R]])

}
