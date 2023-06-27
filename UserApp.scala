import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

// Define states for FSM
sealed trait UserState
case object Idle extends UserState
case object Active extends UserState

// Define events for FSM
sealed trait UserEvent
case object Start extends UserEvent
case object Stop extends UserEvent
case class CustomCommand(command: String) extends UserEvent

// Define messages for persistence
case class UserCommand(command: String)

// Define actor for FSM and persistence
class UserActor extends Actor with ActorLogging {
  var state: UserState = Idle
  var data: Option[String] = None

  def receive: Receive = {
    case Start =>
      state = Active
      log.info("User is now active.")
      sender() ! "Start message processed"
    case Stop =>
      state = Idle
      log.info("User is now idle.")
      sender() ! "Stop message processed"
    case UserCommand(command) =>
      data = Some(command)
      log.info("User received custom command: {}", command)
      sender() ! s"Custom command '$command' processed"
    case unknownMessage =>
      log.info("Unknown message received: {}", unknownMessage)
      sender() ! "Unknown message"
  }
}

object UserApp extends App {
  // Create actor system
  val system = ActorSystem("UserSystem")

  // Create an instance of the actor
  val userActor = system.actorOf(Props[UserActor], "userActor")

  // Send messages to the userActor using ask pattern
  implicit val timeout: Timeout = Timeout(5.seconds)

  val startResponse = userActor ? Start
  startResponse.map { response =>
    println(s"Start response: ${response}")
  }(system.dispatcher)

  val stopResponse = userActor ? Stop
  stopResponse.map { response =>
    println(s"Stop response: ${response}")
  }(system.dispatcher)

  val customCommand = "Custom command"
  val customCommandResponse = userActor ? UserCommand(customCommand)
  customCommandResponse.map { response =>
    println(s"Custom command response: ${response}")
  }(system.dispatcher)

  // Terminate actor system after a delay
  system.scheduler.scheduleOnce(delay = 2.seconds) {
    system.terminate()
  }(system.dispatcher)
}