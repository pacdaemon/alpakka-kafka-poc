//#full-example
package io.sciencebird

import akka.Done
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{CommitterSettings, ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.sciencebird.AkkaQuickstart.{producerSettings, topic}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

//#main-class
object AkkaQuickstart extends App {
  val system: ClassicActorSystem = ClassicActorSystem("producer-test")
  implicit val typedSystem: ActorSystem[Nothing] = system.toTyped

  val config          = system.settings.config.getConfig("our-kafka-consumer")
  val producerConfig  = system.settings.config.getConfig("our-kafka-producer")
  val consumerSettings = ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
    .withGroupId("g1")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true") // avoid re-reading all messages in the queue
  val topic                         = "test"
  implicit val ec: ExecutionContext = system.dispatcher

  val committerSettings = CommitterSettings(system)
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
    typedSystem.log.info(s"Received $key : $value")
    Future.successful(Done)
  }

  val producerSettings =
    ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  val limit = 15 // number of rounds
  val delay = 2 // interval between rounds
  system.spawn(
    ProducerActor(limit, delay),
    "producer-system"
  )

  Thread.sleep(delay * (limit + 1) * 1000)
  consumerControl.shutdown()
  sys.exit(0)
}

object ProducerActor {
  import scala.concurrent.duration._

  sealed trait ProducerMessage
  case object Tick extends ProducerMessage

  def apply(
    limit: Int,
    delay: Int
  )(implicit ec: ExecutionContext, mat: Materializer): Behavior[ProducerMessage] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { scheduler =>
        context.log.debug("starting timer")
        var counter = 0
        scheduler.startTimerWithFixedDelay(counter, Tick, delay seconds)

        def terminate: Behavior[ProducerMessage] = {
          scheduler.cancelAll()
          Behaviors.stopped
        }

        Behaviors.receiveMessage {
          case Tick =>
            if(counter == limit) terminate
            else {
              context.log.debug(s"Starting round: $counter")
              Source(0 to counter * 1000)
                .map(_.toString)
                .map(value => new ProducerRecord[String, String](topic, counter.toString, value))
                .runWith(Producer.plainSink(producerSettings))
                .map(_ => counter += 1)

              Behaviors.same
            }
        }
      }
    }
  }

}
