package com.jcp.simscala.resource

import com.jcp.simscala.event.{ ResourceAcquired, ResourceRelease, ResourceRequest }
import com.jcp.simscala.resource.ResourceAccess.AccessResponse

trait ResourceAccess[R <: Resource] {
  def resource: R
  def request(request: ResourceRequest[R]): AccessResponse[R]
  def release(release: ResourceRelease[R]): AccessResponse[R]
}

object ResourceAccess {
  case class AccessResponse[R <: Resource](newState: ResourceAccess[R], acquired: Option[ResourceAcquired[R]])

  def apply[R <: Resource](resource: R): FifoResourceAccess[R] = resource.accessType match {
    case FifoAccessType => FifoResourceAccess(resource)
  }
}
