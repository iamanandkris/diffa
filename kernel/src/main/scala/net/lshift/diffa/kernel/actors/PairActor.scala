/**
 * Copyright (C) 2010 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lshift.diffa.kernel.actors

import org.slf4j.{Logger, LoggerFactory}
import org.joda.time.DateTime
import net.lshift.diffa.kernel.events.{PairChangeEvent, VersionID}
import net.lshift.diffa.kernel.differencing.{DateConstraint, DifferencingListener, VersionPolicy, Correlation}
import se.scalablesolutions.akka.actor.{ActorRef, ActorRegistry, Actor}
import net.lshift.diffa.kernel.config.ConfigStore
import net.lshift.diffa.kernel.participants.{ParticipantFactory, UpstreamParticipant, DownstreamParticipant}

class PairActor(val pairKey:String,
                val policy:VersionPolicy,
                val participantFactory:ParticipantFactory,
                val config:ConfigStore) extends Actor {

  val logger:Logger = LoggerFactory.getLogger(getClass)

  self.id_=(pairKey)

  def receive = {
    case ChangeMessage(event) => {
      logger.info("Processing change event: " + event)
      policy.onChange(event)
    }
    case DifferenceMessage(dates, listener) => {
      logger.info("Running difference report for: " + dates)
      // TODO This could probably get cached
      val pair = config.getPair(pairKey)
      val us = participantFactory.createUpstreamParticipant(pair.upstream.url)
      val ds = participantFactory.createDownstreamParticipant(pair.downstream.url)
      val sync = policy.difference(pairKey, dates, us, ds, listener)
      self.reply(sync)
      logger.info("Completed difference report for: " + dates)
    }
    case _ => logger.error("received unknown message")
  }
}

case class ActorMessage
case class ChangeMessage(event:PairChangeEvent)
case class DifferenceMessage(dates:DateConstraint, listener:DifferencingListener)

trait ChangeEventClient {

  def propagateChangeEvent(event:PairChangeEvent) : Unit

  def syncPair(pairKey:String, dates:DateConstraint, listener:DifferencingListener) : Boolean
}

class DefaultChangeEventClient extends ChangeEventClient {

  val log = LoggerFactory.getLogger(getClass)

  def propagateChangeEvent(event:PairChangeEvent) = withActor(event.id.pairKey, (a:ActorRef) => a ! ChangeMessage(event))

  def syncPair(pairKey:String, dates:DateConstraint, listener:DifferencingListener) = {
    def handleMessage(a:ActorRef) = {
      val result = a !! DifferenceMessage(dates, listener)
      result match {
        case Some(b) => b.asInstanceOf[Boolean]
        case None => {
          log.error("Message timeout")
          throw new RuntimeException("Message timeout")
        }
      }
    }
    withActor(pairKey,handleMessage)
  }

  def withActor[T](pairKey:String, f:Function1[ActorRef,T]) : T = {
    val actors = ActorRegistry.actorsFor(pairKey)
    actors.length match {
      case 1 => f(actors(0))
      case 0 => {
        log.error("Could not resolve actor for key: " + pairKey)
        throw new RuntimeException("Unresolvable pair: " + pairKey)
      }
      case x => {
        log.error("Too many actors for key: " + pairKey + "; actors = " + x)
        throw new RuntimeException("Too many actors: " + pairKey)
      }
    }
  }
}
