import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.io.Tcp.{Bound, Event}
import akka.io.{IO, Tcp}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Server {

  sealed trait Command extends Event

  final object Start extends Event

  def behavior: Behavior[Event] = Behaviors.receivePartial {
    case (context, Start) =>
      import akka.actor.typed.scaladsl.adapter._
      import akka.io.Tcp._

      implicit val system: akka.actor.ActorSystem = context.system.toUntyped
      IO(Tcp) ! Bind(context.self.toUntyped, new InetSocketAddress("localhost", 3000))

      Behaviors.same
    case (context, Bound(_)) ⇒
      println("Waiting for connections")
      Behavior.same
  }
}

object Root {
  def behavior: Behavior[NotUsed] = Behaviors.setup { context =>
    val endpoint = context.spawn(Server.behavior, "endpoint")
    endpoint ! Server.Start

    Behaviors.receiveSignal {
      case (_, Terminated(_)) ⇒
        Behaviors.stopped
    }
  }
}

object Main extends App {
  val system: ActorSystem[NotUsed] = ActorSystem(Root.behavior, "my-actor-system")
  Await.result(system.whenTerminated, Duration.Inf)
}