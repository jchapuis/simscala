package com.jcp.simscala.resource

trait Resource {
  def name: String
  def capacity: Int
  def accessType: ResourceAccessType
}



