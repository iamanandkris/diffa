/**
 * Copyright (C) 2010-2011 LShift Ltd.
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

package net.lshift.diffa.messaging.json


import net.lshift.diffa.kernel.frontend.Changes
import JSONEncodingUtils._
import net.lshift.diffa.kernel.frontend.wire.EventRegistry._
import net.lshift.diffa.kernel.participants.EventFormatMapper

/**
 * Protocol handler for change requests.
 */

class ChangesHandler(val frontend: Changes,
                     inboundURL: String,
                     eventFormatMapper: EventFormatMapper) extends AbstractJSONHandler {

  override val contentType = eventFormatMapper.contentType

  protected val endpoints = Map(
    inboundURL -> skeleton((eventFormatMapper.map(_: String, inboundURL)
                             .foreach { wireEvent => frontend.onChange(resolveEvent(wireEvent)) })
                           andThen (_ => serializeEmptyResponse()))
  )
}