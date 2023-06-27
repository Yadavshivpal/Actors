import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor._

class Worker extends Actor {
  def receive: Receive = {
    case Task(num) if num % 2 == 0 =>
      println(s"Worker ${self.path.name}: Processing even number: $num")
    case Task(num) =>
      throw new RuntimeException(s"Worker ${self.path.name}: Processing odd number: $num")
  }
}

case class Task(num: Int)
case object RestartWorker

object Supervisor extends App {
  val system = ActorSystem("SupervisionSystem")
  val supervisor = system.actorOf(Props[Supervisor], "supervisor")

  supervisor ! Task(1) // Worker1 throws exception
  supervisor ! Task(2) // Worker2 processes successfully
  supervisor ! Task(3) // Worker1 processes successfully
  Thread.sleep(2000)

  supervisor ! Task(4) // Worker2 processes successfully
  Thread.sleep(2000)

  supervisor ! RestartWorker // Manual restart of worker1
  supervisor ! Task(5) // Worker1 processes successfully

  Thread.sleep(2000)
  system.terminate()
}

class Supervisor extends Actor {
  val worker1 = context.actorOf(Props[Worker], "worker1")
  val worker2 = context.actorOf(Props[Worker], "worker2")

  var currentWorker = 0 // Index of the current worker

  def receive: Receive = {
    case msg =>
      val worker = getCurrentWorker()
      worker forward msg
      currentWorker = (currentWorker + 1) % 2
  }

  private def getCurrentWorker(): ActorRef = {
    currentWorker match {
      case 0 => worker1
      case 1 => worker2
    }
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10) {
    case _: RuntimeException =>
      println("Supervisor: Worker failed. Restarting...")
      Restart
    case _: Exception =>
      println("Supervisor: Worker failed. Escalating...")
      Escalate
  }

  override def preStart(): Unit = {
    println("Supervisor: Started")
  }

  override def postStop(): Unit = {
    println("Supervisor: Stopped")
  }
}