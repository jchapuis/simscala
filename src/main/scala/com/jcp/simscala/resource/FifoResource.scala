package com.jcp.simscala.resource

trait FifoResource extends Resource {
  def accessType: ResourceAccessType = FifoAccessType
}
