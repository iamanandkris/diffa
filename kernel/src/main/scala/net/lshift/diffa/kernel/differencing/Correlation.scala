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

package net.lshift.diffa.kernel.differencing

import reflect.BeanProperty
import org.joda.time.DateTime
import scala.collection.Map
import scala.collection.JavaConversions.MapWrapper
import scala.collection.JavaConversions.asMap
import collection.mutable.HashMap

// Base type for upstream and downstream correlations allowing pairs to be managed
case class Correlation(
  @BeanProperty var oid:java.lang.Integer,
  @BeanProperty var pairing:String,
  @BeanProperty var id:String,
  // TODO [#2] the attributes go into the index, not the DB
  var upstreamAttributes:Map[String,String],
  var downstreamAttributes:Map[String,String],
  @BeanProperty var lastUpdate:DateTime,
  @BeanProperty var timestamp:DateTime,
  @BeanProperty var upstreamVsn:String,
  @BeanProperty var downstreamUVsn:String,
  @BeanProperty var downstreamDVsn:String,
  @BeanProperty var isMatched:java.lang.Boolean
) {
  def this() = this(null, null, null, null, null, null, null, null, null, null, null)

  // Allocate these in the constructor because of NPE when Hibernate starts mapping this stuff 
  if (upstreamAttributes == null) upstreamAttributes = new HashMap[String,String]
  if (downstreamAttributes == null) downstreamAttributes = new HashMap[String,String]

  // TODO [#2] Can these proxies not be members of this class instead of being created on the stack?
  def getDownstreamAttributes() : java.util.Map[String,String] = {
    if (downstreamAttributes != null) {
      new MapWrapper[String,String](downstreamAttributes)
    } else {
      null
    }
  }

  def getUpstreamAttributes() : java.util.Map[String,String] = {
    if (upstreamAttributes != null) {
      new MapWrapper[String,String](upstreamAttributes)
    } else {
      null
    }
  }


  def setUpstreamAttributes(a:java.util.Map[String,String]) : Unit = upstreamAttributes = asMap(a)
  def setDownstreamAttributes(a:java.util.Map[String,String]) : Unit = downstreamAttributes = asMap(a)
  
}

object Correlation {
  def asDeleted(pairing:String, id:String, lastUpdate:DateTime) =
    Correlation(null, pairing, id, null, null, lastUpdate, new DateTime, null, null, null, true)
}