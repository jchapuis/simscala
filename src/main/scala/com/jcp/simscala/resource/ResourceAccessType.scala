package com.jcp.simscala.resource

sealed trait ResourceAccessType
case object FifoAccessType extends ResourceAccessType
