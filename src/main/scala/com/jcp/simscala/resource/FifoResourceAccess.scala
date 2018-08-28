package com.jcp.simscala.resource

import com.jcp.simscala.event.{ Process, ResourceAcquired, ResourceRelease, ResourceRequest }
import com.jcp.simscala.resource.ResourceAccess.AccessResponse
import com.markatta.timeforscala.Instant
import monocle.macros.GenLens

case class FifoResourceAccess[R <: Resource](resource: R, processes: Set[Process], pending: Seq[ResourceRequest[R]])
  extends ResourceAccess[R] {
  private val processesLens = GenLens[FifoResourceAccess[R]](_.processes)
  private val pendingLens   = GenLens[FifoResourceAccess[R]](_.pending)
  def request(request: ResourceRequest[R]): AccessResponse[R] =
    if (processes.size >= resource.capacity)
      AccessResponse(pendingLens.modify(_ :+ request)(this), None)
    else
      AccessResponse(
        processesLens.modify(_ + request.process)(this),
        Some(resourceAcquired(request.process, request.time))
      )

  def release(release: ResourceRelease[R]): AccessResponse[R] =
    if (processes.contains(release.process)) {
      val removeProcess         = processesLens.modify(_ - release.process)
      val possibleAcquiredEvent = headPendingProcess.map(resourceAcquired(_, release.time))
      val addAnyPending         = processesLens.modify(_ ++ headPendingProcess.toSet)
      val removeAnyPending      = pendingLens.modify(_.drop(1))
      AccessResponse(
        (removeProcess andThen addAnyPending
          andThen removeAnyPending)(this),
        possibleAcquiredEvent
      )
    } else
      AccessResponse(this, None)

  private def headPendingProcess = pending.headOption.map(_.process)

  private def resourceAcquired(process: Process, time: Instant) =
    ResourceAcquired(resource, process, time)

  override def toString: String =
    s"""${resource.name}(acquired: ${if (processes.isEmpty) "none" else processes.map(_.name).mkString(", ")}, pending: ${if (pending.isEmpty)
         "none"
       else
         pending
           .map(_.process)
           .map(_.name)
           .mkString(", ")}""".stripMargin
}

object FifoResourceAccess {
  def apply[R <: Resource](resource: R): FifoResourceAccess[R] = FifoResourceAccess(resource, Set(), Nil)
}
