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

package net.lshift.diffa.kernel.config

import org.junit.Assert._
import org.hibernate.cfg.Configuration
import org.slf4j.{Logger, LoggerFactory}
import net.lshift.diffa.kernel.util.MissingObjectException
import org.hibernate.exception.ConstraintViolationException
import org.junit.{Test, Before}
import scala.collection.Map
import scala.collection.JavaConversions._
import org.joda.time.DateTime

class HibernateConfigStoreTest {
  private val configStore: ConfigStore = HibernateConfigStoreTest.configStore
  private val log:Logger = LoggerFactory.getLogger(getClass)

  val dateCategoryName = "bizDate"
  val dateCategoryLower = new DateTime(1982,4,5,12,13,9,0).toString()
  val dateCategoryUpper = new DateTime(1982,4,6,12,13,9,0).toString()
  val dateRangeCategoriesMap =
    Map(dateCategoryName ->  new RangeCategoryDescriptor("date",dateCategoryLower,dateCategoryUpper))

  val setCategoryValues = Set("a","b","c")
  val setCategoriesMap = Map(dateCategoryName ->  new SetCategoryDescriptor(setCategoryValues))

  val intCategoryName = "someInt"
  val stringCategoryName = "someString"

  val intCategoryType = "int"
  val intRangeCategoriesMap = Map(intCategoryName ->  new RangeCategoryDescriptor(intCategoryType))

  val stringPrefixCategoriesMap = Map(stringCategoryName -> new PrefixCategoryDescriptor(1, 3, 1))

  val UPSTREAM_EP = new Endpoint("TEST_UPSTREAM", "TEST_UPSTREAM_URL", "application/json", null, null, true, dateRangeCategoriesMap)

  val UPSTREAM_EP_ALT = new Endpoint("TEST_UPSTREAM_ALT", "TEST_UPSTREAM_URL_ALT", "application/json", null, null, true, setCategoriesMap)

  val DOWNSTREAM_EP = new Endpoint("TEST_DOWNSTREAM", "TEST_DOWNSTREAM_URL", "application/json", null, null, true, intRangeCategoriesMap)
  
  val DOWNSTREAM_EP_ALT = new Endpoint("TEST_DOWNSTREAM_ALT", "TEST_DOWNSTREAM_URL_ALT", "application/json", null, null, true, stringPrefixCategoriesMap)

  val GROUP_KEY = "TEST_GROUP"
  val GROUP = new PairGroup(GROUP_KEY)
  val VP_NAME = "TEST_VPNAME"
  val MATCHING_TIMEOUT = 120
  val VP_NAME_ALT = "TEST_VPNAME_ALT"
  val PAIR_KEY = "TEST_PAIR"
  val PAIR_DEF = new PairDef(PAIR_KEY, VP_NAME, MATCHING_TIMEOUT, UPSTREAM_EP.name,
    DOWNSTREAM_EP.name, GROUP_KEY)

  val GROUP_KEY_ALT = "TEST_GROUP2"
  val UPSTREAM_RENAMED = "TEST_UPSTREAM_RENAMED"
  val GROUP_RENAMED = "TEST_GROUP_RENAMED"
  val PAIR_RENAMED = "TEST_PAIR_RENAMED"

  val TEST_USER = User("foo","foo@bar.com")

  def declareAll: Unit = {
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    configStore.createOrUpdateEndpoint(UPSTREAM_EP_ALT)
    configStore.createOrUpdateEndpoint(DOWNSTREAM_EP)
    configStore.createOrUpdateEndpoint(DOWNSTREAM_EP_ALT)
    configStore.createOrUpdateGroup(GROUP)
    configStore.createOrUpdatePair(PAIR_DEF)
  }

  @Before
  def setUp: Unit = {
    val s = HibernateConfigStoreTest.sessionFactory.openSession
    s.createCriteria(classOf[Pair]).list.foreach(p => s.delete(p))
    s.createCriteria(classOf[PairGroup]).list.foreach(p => s.delete(p))
    s.createCriteria(classOf[Endpoint]).list.foreach(p => s.delete(p))
    s.flush
    s.close
  }

  def exists (e:Endpoint, count:Int, offset:Int) : Unit = {
    val endpoints = configStore.listEndpoints
    assertEquals(count, endpoints.length)
    assertEquals(e.name, endpoints(offset).name)
    assertEquals(e.url, endpoints(offset).url)
    assertEquals(e.online, endpoints(offset).online)
  }

  def exists (e:Endpoint, count:Int) : Unit = exists(e, count, count - 1)

  @Test
  def testDeclare: Unit = {
    // Declare endpoints
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    exists(UPSTREAM_EP, 1)

    configStore.createOrUpdateEndpoint(DOWNSTREAM_EP)
    exists(DOWNSTREAM_EP, 2)

    // Declare a group
    configStore.createOrUpdateGroup(GROUP)
    val retrGroups = configStore.listGroups
    assertEquals(1, retrGroups.length)
    assertEquals(GROUP_KEY, retrGroups.first.group.key)
    assertEquals(0, retrGroups.first.pairs.length)

    // Declare a pair
    configStore.createOrUpdatePair(PAIR_DEF)
    val retrGroups2 = configStore.listGroups
    assertEquals(1, retrGroups2.length)
    assertEquals(1, retrGroups2.first.pairs.length)
    val retrPair = retrGroups2.first.pairs.first
    assertEquals(PAIR_KEY, retrPair.key)
    assertEquals(UPSTREAM_EP.name, retrPair.upstream.name)
    assertEquals(DOWNSTREAM_EP.name, retrPair.downstream.name)
    assertEquals(GROUP_KEY, retrPair.group.key)
    assertEquals(VP_NAME, retrPair.versionPolicyName)
    assertEquals(MATCHING_TIMEOUT, retrPair.matchingTimeout)
  }

  @Test
  def testUpdateEndpoint: Unit = {
    // Create endpoint
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    exists(UPSTREAM_EP, 1)

    configStore.deleteEndpoint(UPSTREAM_EP.name)
    expectMissingObject("endpoint") {
      configStore.getEndpoint(UPSTREAM_EP.name)
    }
        
    // Change its name
    configStore.createOrUpdateEndpoint(Endpoint(UPSTREAM_RENAMED, UPSTREAM_EP.url, "application/json", "changes", "application/json", true))

    val retrieved = configStore.getEndpoint(UPSTREAM_RENAMED)
    assertEquals(UPSTREAM_RENAMED, retrieved.name)
    assertTrue(retrieved.online)
  }

  @Test
  def testUpdatePair: Unit = {
    declareAll
    configStore.createOrUpdateGroup(new PairGroup(GROUP_KEY_ALT))

    // Rename, change a few fields and swap endpoints by deleting and creating new
    configStore.deletePair(PAIR_KEY)
    expectMissingObject("pair") {
      configStore.getPair(PAIR_KEY)
    }

    configStore.createOrUpdatePair(new PairDef(PAIR_RENAMED, VP_NAME_ALT, Pair.NO_MATCHING,
      DOWNSTREAM_EP.name, UPSTREAM_EP.name, GROUP_KEY_ALT))
    
    val retrieved = configStore.getPair(PAIR_RENAMED)
    assertEquals(PAIR_RENAMED, retrieved.key)
    assertEquals(DOWNSTREAM_EP.name, retrieved.upstream.name) // check endpoints are swapped
    assertEquals(UPSTREAM_EP.name, retrieved.downstream.name)
    assertEquals(VP_NAME_ALT, retrieved.versionPolicyName)
    assertEquals(Pair.NO_MATCHING, retrieved.matchingTimeout)
  }

  @Test
  def testUpdateGroup: Unit = {
    // Create a group
    configStore.createOrUpdateGroup(GROUP)

    // Rename it by deleting and re-creating
    configStore.deleteGroup(GROUP.key)
    expectMissingObject("group") {
      configStore.getGroup(GROUP.key)
    }
    configStore.createOrUpdateGroup(new PairGroup(GROUP_RENAMED))

    val retrieved = configStore.getGroup(GROUP_RENAMED)
    assertEquals(GROUP_RENAMED, retrieved.key)
  }

  @Test
  def testDeleteEndpointCascade: Unit = {
    declareAll

    assertEquals(UPSTREAM_EP.name, configStore.getEndpoint(UPSTREAM_EP.name).name)
    configStore.deleteEndpoint(UPSTREAM_EP.name)
    expectMissingObject("endpoint") {
      configStore.getEndpoint(UPSTREAM_EP.name)
    }
    expectMissingObject("pair") {
      configStore.getPair(PAIR_KEY) // delete should cascade
    }
  }

  @Test
  def testDeletePair: Unit = {
    declareAll

    assertEquals(PAIR_KEY, configStore.getPair(PAIR_KEY).key)
    configStore.deletePair(PAIR_KEY)
    expectMissingObject("pair") {
      configStore.getPair(PAIR_KEY)
    }
  }

  @Test
  def testDeleteGroupCascade: Unit = {
    declareAll

    assertEquals(GROUP_KEY, configStore.getGroup(GROUP_KEY).key)
    configStore.deleteGroup(GROUP_KEY)
    expectMissingObject("group") {
      configStore.getGroup(GROUP_KEY)
    }
    expectMissingObject("pair") {
      configStore.getPair(PAIR_KEY) // delete should cascade
    }
  }

  @Test
  def testDeleteMissing: Unit = {
    expectMissingObject("endpoint") {
      configStore.deleteEndpoint("MISSING_ENDPOINT")
    }

    expectMissingObject("pair") {
      configStore.deletePair("MISSING_PAIR")
    }

    expectMissingObject("group") {
      configStore.deleteGroup("MISSING_GROUP")
    }
  }

  @Test
  def testDeclarePairNullConstraints: Unit = {
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    configStore.createOrUpdateEndpoint(DOWNSTREAM_EP)
    configStore.createOrUpdateGroup(GROUP)

      // TODO: We should probably get an exception indicating that the constraint was null, not that the object
      //       we're linking to is missing.
    expectMissingObject("endpoint") {
      configStore.createOrUpdatePair(new PairDef(PAIR_KEY, VP_NAME, Pair.NO_MATCHING, null, DOWNSTREAM_EP.name, GROUP_KEY))
    }
    expectMissingObject("endpoint") {
      configStore.createOrUpdatePair(new PairDef(PAIR_KEY, VP_NAME, Pair.NO_MATCHING, UPSTREAM_EP.name, null, GROUP_KEY))
    }
    expectMissingObject("group") {
      configStore.createOrUpdatePair(new PairDef(PAIR_KEY, VP_NAME, Pair.NO_MATCHING, UPSTREAM_EP.name, DOWNSTREAM_EP.name, null))
    }
  }

  @Test
  def testRedeclareEndpointSucceeds = {
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    configStore.createOrUpdateEndpoint(Endpoint(UPSTREAM_EP.name, "DIFFERENT_URL", "application/json", "changes", "application/json", false))
    assertEquals(1, configStore.listEndpoints.length)
    assertEquals("DIFFERENT_URL", configStore.getEndpoint(UPSTREAM_EP.name).url)
  }

  @Test
  def testQueryingForAssociatedPairsReturnsNothingForUnusedEndpoint {
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    assertEquals(0, configStore.getPairsForEndpoint(UPSTREAM_EP.name).length)
  }

  @Test
  def testQueryingForAssociatedPairsReturnsPairUsingEndpointAsUpstream {
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    configStore.createOrUpdateEndpoint(DOWNSTREAM_EP)
    configStore.createOrUpdateGroup(new PairGroup(GROUP_KEY))
    configStore.createOrUpdatePair(new PairDef(PAIR_KEY, VP_NAME_ALT, Pair.NO_MATCHING,
                                               UPSTREAM_EP.name, DOWNSTREAM_EP.name, GROUP_KEY))

    val res = configStore.getPairsForEndpoint(UPSTREAM_EP.name)
    assertEquals(1, res.length)
    assertEquals(PAIR_KEY, res(0).key)
  }

  @Test
  def testQueryingForAssociatedPairsReturnsPairUsingEndpointAsDownstream {
    configStore.createOrUpdateEndpoint(UPSTREAM_EP)
    configStore.createOrUpdateEndpoint(DOWNSTREAM_EP)
    configStore.createOrUpdateGroup(new PairGroup(GROUP_KEY))
    configStore.createOrUpdatePair(new PairDef(PAIR_KEY, VP_NAME_ALT, Pair.NO_MATCHING,
                                               UPSTREAM_EP.name, DOWNSTREAM_EP.name, GROUP_KEY))

    val res = configStore.getPairsForEndpoint(DOWNSTREAM_EP.name)
    assertEquals(1, res.length)
    assertEquals(PAIR_KEY, res(0).key)
  }

  @Test
  def rangeCategory = {
    declareAll
    val pair = configStore.getPair(PAIR_KEY)
    assertNotNull(pair.upstream.categories)
    assertNotNull(pair.downstream.categories)
    val us_descriptor = pair.upstream.categories(dateCategoryName).asInstanceOf[RangeCategoryDescriptor]
    val ds_descriptor = pair.downstream.categories(intCategoryName).asInstanceOf[RangeCategoryDescriptor]
    assertEquals("date", us_descriptor.dataType)
    assertEquals(intCategoryType, ds_descriptor.dataType)
    assertEquals(dateCategoryLower, us_descriptor.lower)
    assertEquals(dateCategoryUpper, us_descriptor.upper)
  }

  @Test
  def setCategory = {
    declareAll
    val endpoint = configStore.getEndpoint(UPSTREAM_EP_ALT.name)
    assertNotNull(endpoint.categories)
    val descriptor = endpoint.categories(dateCategoryName).asInstanceOf[SetCategoryDescriptor]
    assertEquals(setCategoryValues, descriptor.values.toSet)
  }

  @Test
  def prefixCategory = {
    declareAll
    val endpoint = configStore.getEndpoint(DOWNSTREAM_EP_ALT.name)
    assertNotNull(endpoint.categories)
    val descriptor = endpoint.categories(stringCategoryName).asInstanceOf[PrefixCategoryDescriptor]
    assertEquals(1, descriptor.prefixLength)
    assertEquals(3, descriptor.maxLength)
    assertEquals(1, descriptor.step)
  }

  @Test
  def testUser = {
    configStore.createOrUpdateUser(TEST_USER)
    val result = configStore.listUsers
    assertEquals(1, result.length)
    assertEquals(TEST_USER, result(0))
    val updated = User(TEST_USER.name, "somethingelse@bar.com")
    configStore.createOrUpdateUser(updated)
    val user = configStore.getUser(TEST_USER.name)
    assertEquals(updated, user)
    configStore.deleteUser(TEST_USER.name)
    val users = configStore.listUsers
    assertEquals(0, users.length)    
  }

  private def expectMissingObject(name:String)(f: => Unit) {
    try {
      f
      fail("Expected MissingObjectException")
    } catch {
      case e:MissingObjectException => assertTrue(
        "Missing Object Exception for wrong object. Expected for " + name + ", got msg: " + e.getMessage,
        e.getMessage.contains(name))
    }
  }

  private def expectConstraintViolation(f: => Unit) {
    try {
      f
      fail("Expected ConstraintViolationException")
    } catch {
      case e:ConstraintViolationException => 
    }
  }
}

object HibernateConfigStoreTest {
  private val config = new Configuration().
          addResource("net/lshift/diffa/kernel/config/Config.hbm.xml").
          setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyDialect").
          setProperty("hibernate.connection.url", "jdbc:derby:target/configStore;create=true").
          setProperty("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver").
          setProperty("hibernate.hbm2ddl.auto", "create-drop")

  val sessionFactory = config.buildSessionFactory
  val configStore = new HibernateConfigStore(sessionFactory)
}
