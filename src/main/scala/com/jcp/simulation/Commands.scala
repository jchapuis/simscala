package com.jcp.simulation
import Context._

object Commands {
  trait SimCommand
  case class StartCommand(simContext: SimContext)                  extends SimCommand
  case class ContinueCommand(payload: Any, simContext: SimContext) extends SimCommand
}
