package com.jcp.simscala.process

import com.jcp.simscala.event.Event.EventName
import com.typesafe.scalalogging.Logger

trait ProcessBehavior extends StartBehavior {
  def name: EventName
  def logger: Logger = Logger(s"Process:$name")
}
