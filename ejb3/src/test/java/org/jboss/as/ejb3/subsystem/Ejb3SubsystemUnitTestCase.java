/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;

/**
 * Test case for testing the integrity of the EJB3 subsystem.
 *
 * This checks the following features:
 * - basic subsystem testing (i.e. current model version boots successfully)
 * - registered transformers transform model and operations correctly between different API model versions
 * - expressions appearing in XML configurations are correctly rejected if so required
 * - bad attribute values are correctly rejected
 *
 * @author Emanuel Muckenhuber
 */

public class Ejb3SubsystemUnitTestCase extends AbstractSubsystemBaseTest {

    private static final AdditionalInitialization ADDITIONAL_INITIALIZATION = AdditionalInitialization.withCapabilities(
            SingletonDefaultRequirement.POLICY.getName());

    public Ejb3SubsystemUnitTestCase() {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return ADDITIONAL_INITIALIZATION;
    }

    @Override
    protected Set<PathAddress> getIgnoredChildResourcesForRemovalTest() {
        Set<PathAddress> ignoredChildren = new HashSet<PathAddress>();
       // ignoredChildren.add(PathAddress.pathAddress(PathElement.pathElement("subsystem", "ejb3"), PathElement.pathElement("passivation-store", "infinispan")));
        return ignoredChildren;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-ejb3_9_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/ejb3.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Test
    public void test15() throws Exception {
        standardSubsystemTest("subsystem15.xml", false);
    }

    /** WFLY-7797 */
    @Test
    public void testPoolSizeAlternatives() throws Exception {
        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = getSubsystemXml();
        final KernelServices ks = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
        assertTrue("Subsystem boot failed!", ks.isSuccessfulBoot());

        PathAddress pa = PathAddress.pathAddress("subsystem", "ejb3").append("strict-max-bean-instance-pool", "slsb-strict-max-pool");

        ModelNode composite = Util.createEmptyOperation("composite", PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get("steps");
        ModelNode writeMax = Util.getWriteAttributeOperation(pa, "max-pool-size", 5);
        ModelNode writeDerive = Util.getWriteAttributeOperation(pa, "derive-size", "none");

        steps.add(writeMax);
        steps.add(writeDerive);

        // none works in combo with max-pool-size
        ModelNode response = ks.executeOperation(composite);
        assertEquals(response.toString(), "success", response.get("outcome").asString());

        validatePoolConfig(ks, pa);

        steps.setEmptyList();

        // Other values fail in combo with max-pool-size
        writeMax.get("value").set(10);
        writeDerive.get("value").set("from-cpu-count");

        steps.add(writeMax);
        steps.add(writeDerive);

        ks.executeForFailure(composite);

        validatePoolConfig(ks, pa);

    }

    @Test
    public void testDefaultPools() throws Exception {
        final String subsystemXml = readResource("subsystem-pools.xml");
        final KernelServices ks = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
        assertTrue("Subsystem boot failed!", ks.isSuccessfulBoot());

        // add a test pool
        String testPoolName = "test-pool";
        final PathAddress ejb3Address = PathAddress.pathAddress("subsystem", "ejb3");
        PathAddress testPoolAddress = ejb3Address.append("strict-max-bean-instance-pool", testPoolName);
        final ModelNode addPool = Util.createAddOperation(testPoolAddress);
        ModelNode response = ks.executeOperation(addPool);
        assertEquals(response.toString(), "success", response.get("outcome").asString());

        // set default-mdb-instance-pool
        writeAndReadPool(ks, ejb3Address, "default-mdb-instance-pool", testPoolName);
        writeAndReadPool(ks, ejb3Address, "default-mdb-instance-pool", null);
        writeAndReadPool(ks, ejb3Address, "default-mdb-instance-pool", null);
        writeAndReadPool(ks, ejb3Address, "default-mdb-instance-pool", "mdb-strict-max-pool");

        // set default-slsb-instance-pool
        writeAndReadPool(ks, ejb3Address, "default-slsb-instance-pool", null);
        writeAndReadPool(ks, ejb3Address, "default-slsb-instance-pool", null);
        writeAndReadPool(ks, ejb3Address, "default-slsb-instance-pool", testPoolName);
        writeAndReadPool(ks, ejb3Address, "default-slsb-instance-pool", testPoolName);
        writeAndReadPool(ks, ejb3Address, "default-slsb-instance-pool", "slsb-strict-max-pool");

        final ModelNode removePool = Util.createRemoveOperation(testPoolAddress);
        response = ks.executeOperation(removePool);
        assertEquals(response.toString(), "success", response.get("outcome").asString());
    }

    /**
     * Verifies that attributes with expression are handled properly.
     * @throws Exception for any test failures
     */
    @Test
    public void testExpressionInAttributeValue() throws Exception {
        final String subsystemXml = readResource("with-expression-subsystem.xml");
        final KernelServices ks = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        final ModelNode ejb3 = ks.readWholeModel().get("subsystem", getMainSubsystemName());

        final String statisticsEnabled = ejb3.get("statistics-enabled").resolve().asString();
        assertEquals("true", statisticsEnabled);

        final String logSystemException = ejb3.get("log-system-exceptions").resolve().asString();
        assertEquals("false", logSystemException);

        final String passByValue = ejb3.get("in-vm-remote-interface-invocation-pass-by-value").resolve().asString();
        assertEquals("false", passByValue);

        final String gracefulTxn = ejb3.get("enable-graceful-txn-shutdown").resolve().asString();
        assertEquals("false", gracefulTxn);

        final String disableDefaultEjbPermission = ejb3.get("disable-default-ejb-permissions").resolve().asString();
        assertEquals("false", disableDefaultEjbPermission);

        final int defaultStatefulSessionTimeout = ejb3.get("default-stateful-bean-session-timeout").resolve().asInt();
        assertEquals(600000, defaultStatefulSessionTimeout);

        final int defaultStatefulAccessTimeout = ejb3.get("default-stateful-bean-access-timeout").resolve().asInt();
        assertEquals(5000, defaultStatefulAccessTimeout);

        final String defaultSlsbInstancePool = ejb3.get("default-slsb-instance-pool").resolve().asString();
        assertEquals("slsb-strict-max-pool", defaultSlsbInstancePool);

        final int defaultSingletonAccessTimeout = ejb3.get("default-singleton-bean-access-timeout").resolve().asInt();
        assertEquals(5000, defaultSingletonAccessTimeout);

        final String defaultSfsbPassivationDisabledCache = ejb3.get("default-sfsb-passivation-disabled-cache").resolve().asString();
        assertEquals("simple", defaultSfsbPassivationDisabledCache);

        final String defaultSfsbCache = ejb3.get("default-sfsb-cache").resolve().asString();
        assertEquals("distributable", defaultSfsbCache);

        final String defaultSecurityDomain = ejb3.get("default-security-domain").resolve().asString();
        assertEquals("domain", defaultSecurityDomain);

        final String defaultResourceAdapterName = ejb3.get("default-resource-adapter-name").resolve().asString();
        assertEquals("activemq-ra.rar", defaultResourceAdapterName);

        final String defaultMissingMethodPermissionDenyAccess = ejb3.get("default-missing-method-permissions-deny-access").resolve().asString();
        assertEquals("false", defaultMissingMethodPermissionDenyAccess);

        final String defaultMdbInstancePool = ejb3.get("default-mdb-instance-pool").resolve().asString();
        assertEquals("mdb-strict-max-pool", defaultMdbInstancePool);

        final String defaultEntityBeanOptimisticLocking = ejb3.get("default-entity-bean-optimistic-locking").resolve().asString();
        assertEquals("true", defaultEntityBeanOptimisticLocking);

        final String defaultEntityBeanInstancePool = ejb3.get("default-entity-bean-instance-pool").resolve().asString();
        assertEquals("entity-strict-max-pool", defaultEntityBeanInstancePool);

        final String defaultDistinctName = ejb3.get("default-distinct-name").resolve().asString();
        assertEquals("myname", defaultDistinctName);

        final String allowEjbNameRegex = ejb3.get("allow-ejb-name-regex").resolve().asString();
        assertEquals("false", allowEjbNameRegex);

        final String cachePassivationStore = ejb3.get("cache").asPropertyList().get(1).getValue().get("passivation-store").resolve().asString();
        assertEquals("infinispan", cachePassivationStore);

        final String mdbDeliveryGroupActive = ejb3.get("mdb-delivery-group").asPropertyList().get(1).getValue().get("active").resolve().asString();
        assertEquals("false", mdbDeliveryGroupActive);

        final ModelNode passivationStore = ejb3.get("passivation-store").asPropertyList().get(0).getValue();
        assertEquals("default", passivationStore.get("bean-cache").resolve().asString());
        assertEquals("ejb", passivationStore.get("cache-container").resolve().asString());
        assertEquals(10, passivationStore.get("max-size").resolve().asInt());

        final ModelNode remotingProfile = ejb3.get("remoting-profile").asPropertyList().get(0).getValue();
        assertEquals("true", remotingProfile.get("exclude-local-receiver").resolve().asString());
        assertEquals("true", remotingProfile.get("local-receiver-pass-by-value").resolve().asString());

        final ModelNode remoteHttpConnection = remotingProfile.get("remote-http-connection").asPropertyList().get(0).getValue();
        assertEquals("http://localhost:8180/wildfly-services", remoteHttpConnection.get("uri").resolve().asString());

        final ModelNode remotingEjbReceiver = remotingProfile.get("remoting-ejb-receiver").asPropertyList().get(0).getValue();
        assertEquals(5000, remotingEjbReceiver.get("connect-timeout").resolve().asInt());
        assertEquals("connection-ref", remotingEjbReceiver.get("outbound-connection-ref").resolve().asString());

        final ModelNode channelCreationOption = remotingEjbReceiver.get("channel-creation-options").asPropertyList().get(0).getValue();
        assertEquals(20, channelCreationOption.get("value").resolve().asInt());

        final String asyncThreadPoolName = ejb3.get("service", "async", "thread-pool-name").resolve().asString();
        assertEquals("default", asyncThreadPoolName);

        final String iiopEnableByDefault = ejb3.get("service", "iiop", "enable-by-default").resolve().asString();
        assertEquals("true", iiopEnableByDefault);
        final String useQualifiedName = ejb3.get("service", "iiop", "use-qualified-name").resolve().asString();
        assertEquals("true", useQualifiedName);

        final ModelNode remote = ejb3.get("service", "remote");
        assertEquals("ejb", remote.get("cluster").resolve().asString());
        assertEquals("false", remote.get("execute-in-worker").resolve().asString());
        assertEquals("default", remote.get("thread-pool-name").resolve().asString());
        assertEquals(20, remote.get("channel-creation-options").asPropertyList().get(0).getValue().get("value").resolve().asInt());

        final ModelNode timerService = ejb3.get("service", "timer-service");
        final String fileDataStorePath = timerService.get("file-data-store").asPropertyList().get(0).getValue().get("path").resolve().asString();
        assertEquals("timer-service-data", fileDataStorePath);

        final ModelNode databaseStore = timerService.get("database-data-store").asPropertyList().get(0).getValue();
        assertEquals("java:global/DataSource", databaseStore.get("datasource-jndi-name").resolve().asString());
        assertEquals("hsql", databaseStore.get("database").resolve().asString());
        assertEquals("mypartition", databaseStore.get("partition").resolve().asString());
        assertEquals("true", databaseStore.get("allow-execution").resolve().asString());
        assertEquals("100", databaseStore.get("refresh-interval").resolve().asString());

        final ModelNode strictMaxBeanInstancePool = ejb3.get("strict-max-bean-instance-pool").asPropertyList().get(0).getValue();
        assertEquals("from-cpu-count", strictMaxBeanInstancePool.get("derive-size").resolve().asString());
        assertEquals(5, strictMaxBeanInstancePool.get("timeout").resolve().asInt());
        assertEquals("MINUTES", strictMaxBeanInstancePool.get("timeout-unit").resolve().asString());

        final ModelNode strictMaxBeanInstancePool2 = ejb3.get("strict-max-bean-instance-pool").asPropertyList().get(1).getValue();
        assertEquals(20, strictMaxBeanInstancePool2.get("max-pool-size").resolve().asInt());

        final ModelNode threadPool = ejb3.get("thread-pool").asPropertyList().get(0).getValue();
        assertEquals(10, threadPool.get("max-threads").resolve().asInt());
        assertEquals(10, threadPool.get("core-threads").resolve().asInt());
    }

    private void writeAndReadPool(KernelServices ks, PathAddress ejb3Address, String attributeName, String testPoolName) {
        ModelNode writeAttributeOperation = testPoolName == null ?
                Util.getUndefineAttributeOperation(ejb3Address, attributeName) :
                Util.getWriteAttributeOperation(ejb3Address, attributeName, testPoolName);
        ModelNode response = ks.executeOperation(writeAttributeOperation);
        assertEquals(response.toString(), "success", response.get("outcome").asString());

        final String expectedPoolName = testPoolName == null ? "undefined" : testPoolName;
        final ModelNode readAttributeOperation = Util.getReadAttributeOperation(ejb3Address, attributeName);
        response = ks.executeOperation(readAttributeOperation);
        final String poolName = response.get("result").asString();
        assertEquals("Unexpected pool name", expectedPoolName, poolName);
    }

    private void validatePoolConfig(KernelServices ks, PathAddress pa) {
        ModelNode ra = Util.createEmptyOperation("read-attribute", pa);
        ra.get("name").set("max-pool-size");
        ModelNode response = ks.executeOperation(ra);
        assertEquals(response.toString(), 5, response.get("result").asInt());
        ra.get("name").set("derive-size");
        response = ks.executeOperation(ra);
        assertFalse(response.toString(), response.hasDefined("result"));
    }

}
