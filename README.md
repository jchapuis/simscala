# SimScala

SimScala is a process-oriented lightweight discrete-event simulation framework for Scala and Akka, inspired by SimPy.

## Summary

SimScala aims at providing a centrally-controlled simulation engine (master/slave) for predictability and determinism while yet allowing for a scalable simulation environment.

Processes in SimScala are implemented by actors. Since Akka actors are asynchronous by nature, in contrast to SimPy's coroutines they can run in parallel, can even be distributed and interact with outside systems, and recover from failures. Simulation actors (processes) therefore have all freedom to carry asynchronous operations as much as they need (e.g. to interface with external services, load up data, etc.).

However in terms of the simulation run itself, full asynchronicity is not desirable. Indeed, time is simulated and the simulation engine needs to collect and process all events belonging to a certain time point before proceeding to the next time point.

In order to guarantee such repeatability in simulation runs, operation of the simulation itself (event generation and exchange) is therefore synchronous (using Akka ask pattern). This does impose a limit on scalability, as events are unfolded one after the other - but in practice is very fast.

Ordering of events is managed with a time-based priority-queue mailbox by the simulation master actor (the environment). Simulation runs are recorded and can be replayed thanks to Akka persistence. If simulation actors interact with outside systems, its the responsibility of the simulation actor to guarantee idempotent and stateless operation across replays.

SimScala allows for switching execution modes between discrete, real-time or delta-time. This is a configuration change and has no impact on simulation code (except when interacting with outside systems, which need to support the difference in behavior entailed by switching between these various modes).

## Main concepts

### Events
In SimScala, all occurrences relevant to the simulation run are represented by events. Events are defined by the following trait:

```scala
trait Event {
  def name: String
  def time: Instant
}
``` 

The following `Event` implementations are available:

#### Process, ProcessEnd

Processes in SimScala represent running tasks or activities. Processes emit events and can spawn child processes and produce an output upon termination. The `Process` event represents the launch of a process. `Process` is defined like so:

```scala
case class Process(processActor: ActorRef,
                   name: EventName,
                   time: Instant,
                   callbackMessageOnEnd: CallbackMessage,
                   parent: Option[Process]) extends Event
```

Processes are implemented by actors, bear a unique name and start at the specific time point. Upon process termination, the process actor is sent the specified callback message to allow for cleanup. Processes all have a parent process (with the sole exception of the root process which bootstraps the whole simulation).

Termination of a process is signalled with a `ProcessEnd` event, defined like so:

```scala
case class ProcessEnd[T](process: Process, time: Instant, value: T) extends Event
```

The `value` field allows for returning a value to the parent process upon termination.

#### DelayedCallbackEvent

In SimScala discrete-event simulation runs, time only moves forward by means of `DelayedCallbackEvent` events, which literally represent a passing a amount of time. Here's the definition:

```scala
case class DelayedCallbackEvent[T](delay: Duration,
                                   creationTime: Instant,
                                   callbackProcess: Process,
                                   callbackMessage: CallbackMessage,
                                   value: T)
  extends Event
```

When the simulation run reaches `creationTime + delay`, it sends the `callbackProcess` actor the specified `callbackMessage`. If the process emitting the `DelayedCallbackEvent` is also the recipient of the callback, this allows simulating elapsing of time during the activity represented by the process.  

#### AllOf, AnyOf, ConditionMatched

Like SimPy, SimScala supports conditional events. `AllOf` and `AnyOf` both are `Condition` events, which represent creation of a condition, and `ConditionMatched` the condition being matched at a certain time-point. Here are their definitions:

```scala
trait Condition extends Event {
  def events: Seq[EventName]
  def callbackProcess: Process
  def callbackMessage: CallbackMessage
}

case class AllOf(events: Seq[EventName], time: Instant, callbackProcess: Process, callbackMessage: CallbackMessage) extends Condition
case class AnyOf(events: Seq[EventName], time: Instant, callbackProcess: Process, callbackMessage: CallbackMessage) extends Condition

case class ConditionMatchedEvent(condition: Condition, time: Instant) extends Event
```

Conditions installed in the environment are evaluated upon each event, and a `ConditionMatchedEvent` is sent to the specified `callbackProcess` once at the same time-point as the match.

For `AllOf` to match, all the specified events must occur, whereas for `AnyOf`, occurrence of only one event triggers the match.   

#### ResourceRequest, ResourceAcquired, ResourceRelease
Processes can compete for resources. A resource is defined by the following trait:

```scala
trait Resource {
  def name: String
  def capacity: Int
  def accessType: ResourceAccessType
}
```

A resource has a name and a certain capacity. Access to the resource is determined by `ResourceAccessType` (at present it only supports `FifoAccessType`).

Requesting a resource "slot" is represented by the `ResourceRequest` event, granting of the resource by the `ResourceAcquired` event and `ResourceRelease` indicates process releasing the resource after usage. These three events all implement the following type:

```scala
sealed trait ResourceEvent[R <: Resource] extends Event {
  def resource: R
  def process: Process
  def time: Instant
}
``` 

#### CompositeEvent
 A composite event is simply a list of events considered as one. This is used for convenience: most commands sent from the simulation supervisor (the [Environment](#Environment)) to processes expects a single event as a response, so the only way to reply with multiple events is to assemble them in an instance of `CompositeEvent`:
 
 ```scala
 case class CompositeEvent(events: List[Event]) extends Event
 ```
 
Composite events are simply decomposed into their constituent events at the level of the supervising actor. 

### EventFactory

A set of factory methods is provided in the `EventFactory` object to create valid events. `EventFactory` can be used directly or its methods inherited by processes by extending the `EventFactoryBehavior` trait. 

Here is a non-exhaustive summary view of the available event creation methods:

```scala
  def process(props: Props, name: EventName, callbackMessage: CallbackMessage)
  def process(processBehavior: ProcessBehavior, callbackMessage: CallbackMessage)
    process(processBehavior, None)
  def processEnd[T](value: T)(implicit SC: SimContext)             
  def delayedCallback[T](delay: com.markatta.timeforscala.Duration, callbackMessage: CallbackMessage, value: T)(
    implicit SC: SimContext
  ) 
  def conditionMatched(condition: Condition)(implicit SC: SimContext) = ConditionMatchedEvent(condition, now)
  def allOf(events: List[EventName], callbackMessage: Any)(implicit SC: SimContext) 
  def anyOf(events: List[EventName], callbackMessage: Any)(implicit SC: SimContext) 
  def requestResource[R <: Resource](resource: R)(implicit SC: SimContext) 
  def releaseResource[R <: Resource](resource: R)(implicit SC: SimContext) 
  def never 
```  
 
### SimContext
`SimContext` represents the current state of the simulation, i.e. the simulation time, running processes, conditions, resources and triggered events. It is defined like so:

```scala
case class SimContext(time: SimTime,
                      processTree: Tree[Process],
                      currentProcessLoc: TreeLoc[Process],
                      conditions: Seq[Condition],
                      resources: Map[Resource, ResourceAccess[_]],
                      triggeredEvents: Set[Event])
```

The simulation supervisor (the [Environment](#Environment)) has a mutable `var context: SimContext` variable which gets set upon each processing of an event. `SimContext` itself is completely immutable, and will be used to create a journal of occurred events and the associated resulting context after each event.    

### Environment 

The environment (terminology borrowed from `SimPy`) is the simulation supervisor. It is defined by the following trait:

```scala
trait Environment {
  def run(until: Option[StopCondition])
  def rewind(to: StopCondition): Event
  def pause()
  def resume()
}
```
 
Currently, the only implementation available is `DiscreteEnvironment`, which is implemented with a `DiscreteEnvironementActor`. But `RealTimeEnvironment` and `DeltaTimeEnvironment` should also ultimately be available. 
 
### SimCommand
Commands are the messages with which the environment actor interacts with processes to drive the simulation. All commands implement the following trait:

```scala
sealed trait SimCommand {
  def simContext: SimContext
}
``` 

This definition indicates that every command carries the context of its emission. This allows processes handling commands to introspect the context and is expected as an implicit parameter of most `EventFactory` methods.

All commands are sent from the environment to process actors with the *ask* pattern, and are expected to return events.

In order to facilitate development of process actors, handling specific of simulation commands can be opted in by extending the corresponding *behaviors*. `EventFactory` indeed offers a method to create a process based solely on implementation of *behaviors*, in effect hiding actor instantiation from the process implementer. Note that behaviors are just helpers to reduce boilerplate and offer development guidance: direct usage of actors and even retrofitting simulation on an existing actor is possible by simply handling the relevant simulation commands.    
 
The following command and behavior types exist:

| Command |Occurrence|Description|Signature|
| :-------: |:----------:|:-----------:|:----:|
|`StartCommand`|upon `Process` event|This command is sent to the referenced actor in the `Process` event|`case class StartCommand(simContext: SimContext)`| 
|`CallbackCommand`|upon `DelayedCallbackEvent` and `ProcessEnd` events|This command is sent to `callbackProcess` actor in the `DelayedCallbackCommand` event, and the process actor for the `ProcessEnd` event respectively|`case class CallbackCommand[T](message: CallbackMessage, value: T, simContext: SimContext)`|
|`ResourceAcquiredCommand`|upon `ResourceAcquired` event|This command is sent to `process` actor in the corresponding `ResourceRequest` event|`case class ResourceAcquiredCommand[R <: Resource](resource: R, simContext: SimContext)`
|`ConditionMatchedCommand`|upon `ConditionMatchedEvent` event|This command is sent to `callbackProcess` actor in the corresponding `Condition` event|`case class ConditionMatchedEvent(condition: Condition, time: Instant)` 
|`InterruptCommand`|upon interruption/pause of the simulation run||
|`ResumeCommand`|upon resuming the simulation run||

## Example
Here a complete example featuring 10 shuttles. Each shuttle drives for a maximum of an hour, split in 15 minutes increments. Between each increment, the shuttle needs to recharge it battery during 5 minutes at the charging station. The charging station only has 3 slots, so shuttles are competing among themselves for it.

```scala
object ChargingShuttles {
  implicit val system = ActorSystem("ChargingShuttles")

  def main(args: Array[String]): Unit = {
    val environment =
      DiscreteEnvironment(EventFactory.rootProcess(ShuttleDispatch((1 to 10).map(Shuttle).toList)))
    environment.run()
    Await.ready(system.whenTerminated, scala.concurrent.duration.Duration.Inf)
  }

  case class ShuttleDispatch(shuttles: List[Shuttle])
    extends ProcessBehavior
    with ConditionBehavior
    with CallbackBehavior
    with EventFactoryBehavior {
    def name: EventName      = "ShuttleDispatch"
    def shuttleDone(id: Int) = s"shuttle${id}Done"
    val AllShuttlesDone      = "shuttlesDone"

    def start(implicit SC: SimContext): Event = {
      val shuttleProcesses = shuttles.map(shuttle => process(shuttle, shuttleDone(shuttle.id)))
      val endCondition     = allOf(shuttleProcesses.map(_.endEventName), AllShuttlesDone)
      CompositeEvent(shuttleProcesses :+ endCondition)
    }

    override def receiveConditionMatched(condition: Condition)(implicit SC: SimContext): Event = {
      logger.info("All shuttles done, exiting")
      processEnd
    }

    def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext): Event = callback match {
      case shuttleDone: String => {
        logger.info(shuttleDone)
        Event.Never
      }
    }
  }

  case class Shuttle(id: Int) extends ProcessBehavior with CallbackBehavior with EventFactoryBehavior {
    val ChargingDone     = "chargingDone"
    val DrivingDone      = "rideDone"
    val DrivingIncrement = 15 minutes
    val MaxDrivingTime   = 1 hour
    var totalDrivingTime = Duration.Zero

    def start(implicit simContext: SimContext) = charge(simContext)

    def receiveCallback[T](callback: Any, value: T)(implicit simContext: SimContext) =
      callback match {
        case ChargingDone =>
          logger.info("Charging done, start driving")
          drive(DrivingIncrement)
        case DrivingDone =>
          totalDrivingTime = totalDrivingTime.plus(DrivingIncrement)
          if (totalDrivingTime >= MaxDrivingTime) {
            logger.info(s"Total time driven $totalDrivingTime, heading to garage")
            processEnd
          } else {
            logger.info("Driving done, need charging")
            charge
          }
      }

    private def charge(implicit simContext: SimContext) = process(Charge(name, 5 minutes), ChargingDone)
    private def drive(duration: Duration)(implicit simContext: SimContext) =
      process(Drive(name, DrivingIncrement), DrivingDone)

    def name: EventName = s"Shuttle$id"
  }

  case class Charge(shuttleId: String, duration: Duration)
    extends ProcessBehavior
    with CallbackBehavior
    with ResourceBehavior
    with EventFactoryBehavior {
    def start(implicit SC: SimContext) = {
      logger.info("Requesting charging station")
      requestResource(ChargingStation)
    }

    def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext) = {
      logger.info("Finished charging")
      CompositeEvent(releaseResource(ChargingStation), processEnd)
    }

    def resourceAcquired[R](resource: R)(implicit SC: SimContext): Event = {
      logger.info("Start charging")
      delayedCallback(duration, None, None)
    }

    override def name: EventName = s"Charge$shuttleId"
  }

  case class Drive(shuttleId: String, duration: Duration)
    extends ProcessBehavior
    with CallbackBehavior
    with EventFactoryBehavior {
    def name: EventName = s"Drive$shuttleId"

    def start(implicit SC: SimContext): Event = {
      logger.info(s"Start driving $shuttleId until ${SC.time.now + duration}")
      delayedCallback(duration, None, None)
    }

    def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext): Event = {
      logger.info(s"Finished driving $shuttleId")
      processEnd
    }
  }

  case object ChargingStation extends FifoResource {
    override def name: String  = "ChargingStation"
    override def capacity: Int = 3
  }

}
```

## Missing
This is still very early-stage and experimental. Here are some missing points:
 1. Stabilize API
 1. Full coverage (no tests for now, as the API was still in flux)
 1. Example implementation of all SimPy's examples
 1. Pause/resume functionality
 1. Record/replay of runs using akka event journaling 
 1. Real-time and delta-time modes
 1. Stochastic generation methods
 1. ...