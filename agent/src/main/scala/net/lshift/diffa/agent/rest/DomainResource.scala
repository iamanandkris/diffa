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

package net.lshift.diffa.agent.rest

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.{UriInfo, Context}
import javax.ws.rs.{PathParam, Path}
import net.lshift.diffa.kernel.client.ActionsClient
import net.lshift.diffa.kernel.differencing.SessionManager
import net.lshift.diffa.kernel.diag.DiagnosticsManager
import net.lshift.diffa.kernel.actors.PairPolicyClient
import net.lshift.diffa.kernel.config.DomainConfigStore
import net.lshift.diffa.kernel.frontend.{Changes, Configuration}

@Path("/{domain}")
@Component
class DomainResource {

  @Context var uriInfo:UriInfo = null

  @Autowired var config:Configuration = null
  @Autowired var actionsClient:ActionsClient = null
  @Autowired var sessionManager:SessionManager = null
  @Autowired var diagnosticsManager:DiagnosticsManager = null
  @Autowired var pairPolicyClient:PairPolicyClient = null
  @Autowired var domainConfigStore:DomainConfigStore = null
  @Autowired var changes:Changes = null

  @Path("/config")
  def getConfigResource(@Context uri:UriInfo,
                        @PathParam("domain") domain:String) = new ConfigurationResource(config, domain, uri)

  @Path("/diffs")
  def getDifferencesResource(@Context uri:UriInfo,
                             @PathParam("domain") domain:String) = new DifferencesResource(sessionManager, domain, uri)

  @Path("/escalations")
  def getEscalationsResource(@PathParam("domain") domain:String) = new EscalationsResource(config, domain)

  @Path("/actions")
  def getActionsResource(@Context uri:UriInfo,
                         @PathParam("domain") domain:String) = new ActionsResource(actionsClient, domain, uri)

  @Path("/diagnostics")
  def getDiagnosticsResource(@PathParam("domain") domain:String) = new DiagnosticsResource(diagnosticsManager, config, domain)

  @Path("/scanning")
  def getScanningResource(@PathParam("domain") domain:String) = new ScanningResource(pairPolicyClient, config, domainConfigStore, diagnosticsManager, domain)

  @Path("/changes")
  def getChangesResource(@PathParam("domain") domain:String) = new ChangesResource(changes, domain)

}