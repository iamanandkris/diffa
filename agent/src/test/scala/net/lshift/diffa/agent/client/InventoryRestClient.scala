package net.lshift.diffa.agent.client

/**
 * Copyright (C) 2012 LShift Ltd.
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

import com.sun.jersey.api.client.ClientResponse
import net.lshift.diffa.client.RestClientParams

class InventoryRestClient(serverRootUrl:String, domain:String, params: RestClientParams = RestClientParams.default)
    extends DomainAwareRestClient(serverRootUrl, domain, "domains/{domain}/inventory/", params) {

  def uploadInventory(epName:String, content:String) {
    val path = resource.path(epName)
    val response = path.entity(content, "text/csv").post(classOf[ClientResponse])
    val status = response.getClientResponseStatus
    status.getStatusCode match {
      case 202     => // Successfully submitted (202 is "Accepted")
      case x:Int   => handleHTTPError(x,path, status)
    }
  }
}
