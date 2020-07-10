//#full-example
package io.sciencebird

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.delivery.ConsumerController.RegisterToProducerController
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Consumer._
import akka.kafka.scaladsl.{ Committer, Consumer, Producer }
import akka.kafka.{ CommitterSettings, ConsumerSettings, ProducerSettings, Subscriptions }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import io.sciencebird.GreeterMain.SayHello
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{ ProducerConfig, ProducerRecord }
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer, StringSerializer }

import scala.concurrent.{ ExecutionContext, Future }

//#greeter-actor
object Greeter {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    context.log.info("Hello {}!", message.whom)
    //#greeter-send-messages
    message.replyTo ! Greeted(message.whom, context.self)
    //#greeter-send-messages
    Behaviors.same
  }
}
//#greeter-actor

//#greeter-bot
object GreeterBot {

  def apply(max: Int): Behavior[Greeter.Greeted] =
    bot(0, max)

  private def bot(greetingCounter: Int, max: Int): Behavior[Greeter.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max)
        Behaviors.stopped
      else {
        message.from ! Greeter.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}
//#greeter-bot

//#greeter-main
object GreeterMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      //#create-actors
      val greeter = context.spawn(Greeter(), "greeter")
      //#create-actors

      Behaviors.receiveMessage { message =>
        //#create-actors
        val replyTo = context.spawn(GreeterBot(max = 3), message.name)
        //#create-actors
        greeter ! Greeter.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
}
//#greeter-main

//#main-class
object AkkaQuickstart extends App {
  //#actor-system
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "AkkaQuickStart")
  //#actor-system

  //#main-send-messages
  greeterMain ! SayHello("Charles")
  //#main-send-messages

  implicit val system = greeterMain.classicSystem
  val config          = system.settings.config.getConfig("our-kafka-consumer")
  val producerConfig  = system.settings.config.getConfig("our-kafka-producer")
  val consumerSettings = ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
    .withGroupId("g1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  val topic                         = "test"
  implicit val ec: ExecutionContext = system.dispatcher

  val committerSettings = CommitterSettings(system)

//  val control: DrainingControl[Done] =
//    Consumer
//      .committableSource(consumerSettings, Subscriptions.topics(topic))
//      .mapAsync(1) { msg =>
//        business(msg.record.key, msg.record.value)
//          .map(_ => msg.committableOffset)
//      }
//      .toMat(Committer.sink(committerSettings))(DrainingControl.apply)
//      .run()

  val (consumerControl, streamComplete) =
    Consumer
      .plainSource(
        consumerSettings,
        Subscriptions.topics(topic)
      )
      .mapAsync(1) { msg =>
        business(msg.key(), msg.value())
      }
      .toMat(Sink.ignore)(Keep.both)
      .run()

  def business(key: String, value: String): Future[Done] = {
    println(s"Received $key : $value")
    Future.successful(Done)
  }

  val producerSettings =
    ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  (1 to 3).foreach { n =>
    println(s"Starting round: $n")
    Source(n * 1000 to n * 1100)
      .map(_.toString)
      .map(value => new ProducerRecord[String, String](topic, value))
      .runWith(Producer.plainSink(producerSettings))
    println(s"Finishing round $n")
    Thread.sleep(1000)
  }

  Thread.sleep(200 * 1000)
  consumerControl.shutdown()

}
//#main-class
//#full-example
