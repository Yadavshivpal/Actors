import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

case class Message(payload: String)

class WorkerActor extends Actor {
  override def receive: Receive = {
    case Message(payload) =>
      println(s"${self.path.name} received message: $payload")
  }
}

class MasterActor extends Actor {
  private var router: Router = _

  override def preStart(): Unit = {
    val workerProps = Props[WorkerActor]
    val routees = Vector.fill(5){
      val worker = context.actorOf(workerProps)
      ActorRefRoutee(worker)
    }
    router = Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive: Receive = {
    case message: Message =>
      router.route(message, sender())
  }
}

object ActorRouting extends App {
  val system = ActorSystem("MyActorSystem")
  val masterActor = system.actorOf(Props[MasterActor], "masterActor")

  masterActor ! Message("Hello")
  masterActor ! Message("World")
  masterActor ! Message("Namaste")

  Thread.sleep(1000)
  system.terminate()
}