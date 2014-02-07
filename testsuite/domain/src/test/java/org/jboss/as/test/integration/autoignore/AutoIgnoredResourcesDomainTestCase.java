/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.autoignore;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateFailedResponse;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Tests the
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AutoIgnoredResourcesDomainTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static final ModelNode ROOT_ADDRESS = new ModelNode().setEmptyList();
    private static final ModelNode MASTER_ROOT_ADDRESS = new ModelNode().add(HOST, "master");
    private static final ModelNode SLAVE_ROOT_ADDRESS = new ModelNode().add(HOST, "slave");

    static {
        ROOT_ADDRESS.protect();
        MASTER_ROOT_ADDRESS.protect();
        SLAVE_ROOT_ADDRESS.protect();
    }

    private static final String EXTENSION_JMX = "org.jboss.as.jmx";
    private static final String EXTENSION_LOGGING = "org.jboss.as.logging";
    private static final String EXTENSION_MAIL = "org.jboss.as.mail";
    private static final String EXTENSION_NAMING = "org.jboss.as.naming";
    private static final String EXTENSION_POJO = "org.jboss.as.pojo";
    private static final String EXTENSION_SAR = "org.jboss.as.sar";

    private static final String PROFILE1 = "profile1";
    private static final String PROFILE2 = "profile2";
    private static final String PROFILE3 = "profile3";

    private static final String SOCKETS1 = "sockets1";
    private static final String SOCKETS2 = "sockets2";
    private static final String SOCKETS3 = "sockets3";
    private static final String SOCKETSA = "socketsA";

    private static final String GROUP1 = "group1";
    private static final String GROUP2 = "group2";

    private static final String SERVER1 = "server1";

    @BeforeClass
    public static void setupDomain() throws Exception {
        //Make all the configs read-only so we can stop and start when we like to reset
        DomainTestSupport.Configuration config = DomainTestSupport.Configuration.create(AutoIgnoredResourcesDomainTestCase.class.getSimpleName(),
                "domain-configs/domain-auto-ignore.xml", "host-configs/host-auto-ignore-master.xml", "host-configs/host-auto-ignore-slave.xml",
                true, true, true);
        testSupport = DomainTestSupport.create(config);
        // Start!
        testSupport.start();
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
        testSupport = null;
    }

    private DomainClient masterClient;
    private DomainClient slaveClient;

    @Before
    public void setup() throws Exception {
        masterClient = domainMasterLifecycleUtil.getDomainClient();
        slaveClient = domainSlaveLifecycleUtil.getDomainClient();
    }

    /////////////////////////////////////////////////////////////////
    // These tests check that a simple operation on the slave server
    // config pulls down the missing data from the DC

    @Test
    public void test00_CheckInitialBootExclusions() throws Exception {
        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1);
        checkSystemProperties(0);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test01_ChangeSlaveServerConfigSocketBindingGroupOverridePullsDownDataFromDc() throws Exception {
        validateResponse(slaveClient.execute(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), SOCKET_BINDING_GROUP, SOCKETSA)), false);

        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA);
        checkSystemProperties(0);
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test02_ChangeSlaveServerConfigGroupPullsDownDataFromDc() throws Exception {
        validateResponse(slaveClient.execute(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), GROUP, GROUP2)), false);

        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(0);
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test03_AddServerGroupAndServerConfigPullsDownDataFromDc() throws Exception {
        ModelNode addGroupOp = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "testgroup")));
        addGroupOp.get(PROFILE).set(PROFILE3);
        addGroupOp.get(SOCKET_BINDING_GROUP).set(SOCKETS3);
        validateResponse(masterClient.execute(addGroupOp), false);

        //New data should not be pushed yet since nothing on the slave uses it
        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(0);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        ModelNode addConfigOp = Util.createAddOperation(PathAddress.pathAddress(getSlaveServerConfigAddress("testserver")));
        addConfigOp.get(GROUP).set("testgroup");
        validateResponse(slaveClient.execute(addConfigOp), false);

        //Now that we have a group using the new data it should be pulled down
        checkSlaveProfiles(PROFILE1, PROFILE2, PROFILE3);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO, EXTENSION_JMX, EXTENSION_SAR);
        checkSlaveServerGroups(GROUP1, GROUP2, "testgroup");
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2, SOCKETS3);
        checkSystemProperties(0);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test04_RestartDomainAndReloadReadOnlyConfig() throws Exception {
        //Clean up after ourselves for the next round of tests /////////////
        restartDomainAndReloadReadOnlyConfig();
    }

    /////////////////////////////////////////////////////////////////
    // These tests use a composite to obtain the DC lock, and check
    // that an operation on the slave server config pulls down the
    // missing data from the DC

    @Test
    public void test10_ChangeSlaveServerConfigSocketBindingGroupOverridePullsDownDataFromDcWithDcLockTaken() throws Exception {
        validateResponse(slaveClient.execute(createDcLockTakenComposite(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), SOCKET_BINDING_GROUP, SOCKETSA))), false);

        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA);
        checkSystemProperties(1); //Composite added a property
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test11_ChangeSlaveServerConfigGroupPullsDownDataFromDcWithDcLockTaken() throws Exception {
        validateResponse(slaveClient.execute(createDcLockTakenComposite(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), GROUP, GROUP2))), false);

        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(2); //Composite added a property
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test12_AddServerGroupAndServerConfigPullsDownDataFromDcWithDcLockTaken() throws Exception {
        ModelNode addGroupOp = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "testgroup")));
        addGroupOp.get(PROFILE).set(PROFILE3);
        addGroupOp.get(SOCKET_BINDING_GROUP).set(SOCKETS3);
        validateResponse(masterClient.execute(createDcLockTakenComposite(addGroupOp)), false);

        //New data should not be pushed yet since nothing on the slave uses it
        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(3); //Composite added a property
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        ModelNode addConfigOp = Util.createAddOperation(PathAddress.pathAddress(getSlaveServerConfigAddress("testserver")));
        addConfigOp.get(GROUP).set("testgroup");
        validateResponse(slaveClient.execute(createDcLockTakenComposite(addConfigOp)), false);

        //Now that we have a group using the new data it should be pulled down
        checkSlaveProfiles(PROFILE1, PROFILE2, PROFILE3);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO, EXTENSION_JMX, EXTENSION_SAR);
        checkSlaveServerGroups(GROUP1, GROUP2, "testgroup");
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2, SOCKETS3);
        checkSystemProperties(4); //Composite added a property
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }



    @Test
    public void test13_RestartDomainAndReloadReadOnlyConfig() throws Exception {
        //Clean up after ourselves for the next round of tests /////////////
        restartDomainAndReloadReadOnlyConfig();
    }


    /////////////////////////////////////////////////////////////////
    // These tests use a composite to obtain the DC lock, and check
    // that an operation on the slave server config pulls down the
    // missing data from the DC
    // The first thime this is attempted the operation will roll back
    // The second time it should succeed


    @Test
    public void test20_ChangeSlaveServerConfigSocketBindingGroupOverridePullsDownDataFromDcWithDcLockTakenAndRollback() throws Exception {
        validateFailedResponse(slaveClient.execute(createDcLockTakenCompositeWithRollback(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), SOCKET_BINDING_GROUP, SOCKETSA))));

        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1);
        checkSystemProperties(0);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        validateResponse(slaveClient.execute(createDcLockTakenComposite(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), SOCKET_BINDING_GROUP, SOCKETSA))), false);
        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA);
        checkSystemProperties(1); //Composite added a property
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test21_ChangeSlaveServerConfigGroupPullsDownDataFromDcWithDcLockTakenAndRollback() throws Exception {
        validateFailedResponse(slaveClient.execute(createDcLockTakenCompositeWithRollback(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), GROUP, GROUP2))));

        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA);
        checkSystemProperties(1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        validateResponse(slaveClient.execute(createDcLockTakenComposite(Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), GROUP, GROUP2))), false);

        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(2); //Composite added a property
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test22_AddServerGroupAndServerConfigPullsDownDataFromDcWithDcLockTakenAndRollback() throws Exception {
        ModelNode addGroupOp = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "testgroup")));
        addGroupOp.get(PROFILE).set(PROFILE3);
        addGroupOp.get(SOCKET_BINDING_GROUP).set(SOCKETS3);
        validateResponse(masterClient.execute(createDcLockTakenComposite(addGroupOp)), false);

        //New data should not be pushed yet since nothing on the slave uses it
        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(3); //Composite added a property
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        ModelNode addConfigOp = Util.createAddOperation(PathAddress.pathAddress(getSlaveServerConfigAddress("testserver")));
        addConfigOp.get(GROUP).set("testgroup");
        validateFailedResponse(slaveClient.execute(createDcLockTakenCompositeWithRollback(addConfigOp)));
        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2);
        checkSystemProperties(3);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        //Now that we have a group using the new data it should be pulled down
        validateResponse(slaveClient.execute(createDcLockTakenComposite(addConfigOp)), false);
        checkSlaveProfiles(PROFILE1, PROFILE2, PROFILE3);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO, EXTENSION_JMX, EXTENSION_SAR);
        checkSlaveServerGroups(GROUP1, GROUP2, "testgroup");
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETSA, SOCKETS2, SOCKETS3);
        checkSystemProperties(4); //Composite added a property
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    @Test
    public void test23_RestartDomainAndReloadReadOnlyConfig() throws Exception {
        //Clean up after ourselves for the next round of tests /////////////
        restartDomainAndReloadReadOnlyConfig();
    }

    /////////////////////////////////////////////////////////////////
    // These tests test that changing a server group on the DC
    // piggybacks missing data to the slave

    @Test
    public void test30_ChangeServerGroupSocketBindingGroupGetsPushedToSlave() throws Exception {
        ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, GROUP1)).toModelNode(), SOCKET_BINDING_GROUP, SOCKETS2);
        validateResponse(masterClient.execute(op));

        checkSlaveProfiles(PROFILE1);
        checkSlaveExtensions(EXTENSION_LOGGING);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETS2);
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }


    @Test
    public void test31_ChangeServerGroupProfileGetsPushedToSlave() throws Exception {
        ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, GROUP1)).toModelNode(), PROFILE, PROFILE2);
        validateResponse(masterClient.execute(op));

        checkSlaveProfiles(PROFILE1, PROFILE2);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO);
        checkSlaveServerGroups(GROUP1);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETS2);
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));
    }

    /////////////////////////////////////////////////////////////////
    // Test deployments to a server group get picked up by a server
    // switching to it
    @Test
    public void test40_ChangeServerGroupProfileAndGetDeployment() throws Exception {

        JavaArchive deployment = ShrinkWrap.create(JavaArchive.class);
        deployment.addClasses(TestClass.class, TestClassMBean.class);

        File testMarker = new File("target" + File.separator + "testmarker");
        if (testMarker.exists()) {
            testMarker.delete();
        }
        String serviceXml = "<server xmlns=\"urn:jboss:service:7.0\"" +
                            "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                            "   xsi:schemaLocation=\"urn:jboss:service:7.0 jboss-service_7_0.xsd\">" +
                            "   <mbean name=\"jboss:name=test,type=testclassfilemarker\" code=\"org.jboss.as.test.integration.autoignore.TestClass\">" +
                            "       <attribute name=\"path\">" + testMarker.getAbsolutePath() + "</attribute>" +
                            "    </mbean>" +
                            "</server>";
        deployment.addAsManifestResource(new StringAsset(serviceXml), "jboss-service.xml");

        InputStream in = deployment.as(ZipExporter.class).exportAsInputStream();
        masterClient.getDeploymentManager().execute(masterClient.getDeploymentManager().newDeploymentPlan().add("sardeployment.sar", in).deploy("sardeployment.sar").toServerGroup(GROUP2).build());

        ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, GROUP2)).toModelNode(), PROFILE, PROFILE3);
        validateResponse(masterClient.execute(op));

        op = Util.getWriteAttributeOperation(getSlaveServerConfigAddress(SERVER1), GROUP, GROUP2);
        validateResponse(slaveClient.execute(op));

        checkSlaveProfiles(PROFILE1, PROFILE2, PROFILE3);
        checkSlaveExtensions(EXTENSION_LOGGING, EXTENSION_MAIL, EXTENSION_NAMING, EXTENSION_POJO, EXTENSION_JMX, EXTENSION_SAR);
        checkSlaveServerGroups(GROUP1, GROUP2);
        checkSlaveSocketBindingGroups(SOCKETS1, SOCKETS2);
        Assert.assertEquals(RELOAD_REQUIRED, getSlaveServerStatus(SERVER1));

        Assert.assertFalse(testMarker.exists());

        restartSlaveServer(SERVER1);
        Assert.assertEquals("running", getSlaveServerStatus(SERVER1));

        //The mbean should have created this file
        Assert.assertTrue(testMarker.exists());
    }

    /////////////////////////////////////////////////////////////////
    // Private stuff


    private ModelNode createDcLockTakenComposite(ModelNode op) {
        ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();

        ModelNode addProperty = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, String.valueOf(System.currentTimeMillis()))));
        addProperty.get(VALUE).set("xxx");
        composite.get(STEPS).add(addProperty);
        composite.get(STEPS).add(op);
        return composite;
    }

    private ModelNode createDcLockTakenCompositeWithRollback(ModelNode op) {
        ModelNode composite = createDcLockTakenComposite(op);

        ModelNode rollback = Util.getWriteAttributeOperation(SLAVE_ROOT_ADDRESS.clone().add(SYSTEM_PROPERTY, "rollback-does-not-exist" + String.valueOf(System.currentTimeMillis())), VALUE, "xxx");
        composite.get(STEPS).add(rollback);
        return composite;
    }

    private void checkSystemProperties(int size) throws Exception {
        Assert.assertEquals(size, getChildrenOfTypeOnSlave(SYSTEM_PROPERTY).asList().size());
    }

    private void checkSlaveProfiles(String...profiles) throws Exception {
        checkEqualContents(getChildrenOfTypeOnSlave(PROFILE).asList(), profiles);
    }


    private void checkSlaveExtensions(String...extensions) throws Exception {
        checkEqualContents(getChildrenOfTypeOnSlave(EXTENSION).asList(), extensions);
    }

    private void checkSlaveServerGroups(String...groups) throws Exception {
        checkEqualContents(getChildrenOfTypeOnSlave(SERVER_GROUP).asList(), groups);
    }

    private void checkSlaveSocketBindingGroups(String...groups) throws Exception {
        checkEqualContents(getChildrenOfTypeOnSlave(SOCKET_BINDING_GROUP).asList(), groups);
    }

    private ModelNode getChildrenOfTypeOnSlave(String type) throws Exception {
        ModelNode op = Util.createOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(CHILD_TYPE).set(type);
        ModelNode result = slaveClient.execute(op);
        return validateResponse(result);
    }

    private String getSlaveServerStatus(String serverName) throws Exception {
        ModelNode op = Util.getReadAttributeOperation(PathAddress.pathAddress(getSlaveRunningServerAddress(serverName)), "server-state");
        ModelNode result = slaveClient.execute(op);
        return validateResponse(result).asString();
    }

    private ModelNode getSlaveServerConfigAddress(String serverName) {
        return SLAVE_ROOT_ADDRESS.clone().add(SERVER_CONFIG, serverName);
    }

    private ModelNode getSlaveRunningServerAddress(String serverName) {
        return SLAVE_ROOT_ADDRESS.clone().add(SERVER, serverName);
    }

    private void checkEqualContents(List<ModelNode> values, String...expected) {
        HashSet<String> actualSet = new HashSet<String>();
        for (ModelNode value : values) {
            actualSet.add(value.asString());
        }
        HashSet<String> expectedSet = new HashSet<String>(Arrays.asList(expected));
        Assert.assertEquals("Expected " + expectedSet + "; was " + actualSet, expectedSet, actualSet);
    }

    private void restartSlaveServer(String serverName) throws Exception {
        ModelNode op = Util.createOperation(RESTART, PathAddress.pathAddress(getSlaveServerConfigAddress(serverName)));
        op.get(BLOCKING).set(true);
        Assert.assertEquals("STARTED", validateResponse(slaveClient.execute(op), true).asString());
    }

    private void restartDomainAndReloadReadOnlyConfig() throws Exception {
        DomainTestSupport.stopHosts(TimeoutUtil.adjust(30000), domainSlaveLifecycleUtil, domainMasterLifecycleUtil);
        testSupport.stop();

        //Totally reinitialize the domain client
        setupDomain();
        setup();
        //Check we're back to where we were
        test00_CheckInitialBootExclusions();
    }
}
