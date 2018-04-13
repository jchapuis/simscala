package com.jcp.simscala
import Context._

object Commands {
  trait SimCommand
  case class StartCommand(simContext: SimContext)                  extends SimCommand
  case class ContinueCommand(payload: Any, simContext: SimContext) extends SimCommand
}
