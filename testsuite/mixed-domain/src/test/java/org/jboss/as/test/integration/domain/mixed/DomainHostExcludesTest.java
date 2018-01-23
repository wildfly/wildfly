/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLONE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORE_UNUSED_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_PROFILE;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.ExpressionResolverImpl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Base class for tests of the ability of a DC to exclude resources from visibility to a slave.
 *
 * @author Brian Stansberry
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class DomainHostExcludesTest {

    private static final String[] EXCLUDED_EXTENSIONS_6X = {
            "org.wildfly.extension.batch.jberet",
            "org.wildfly.extension.bean-validation",
            "org.wildfly.extension.clustering.singleton",
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.io",
            "org.wildfly.extension.messaging-activemq",
            "org.wildfly.extension.request-controller",
            "org.wildfly.extension.security.manager",
            "org.wildfly.extension.undertow",
            "org.wildfly.iiop-openjdk"
    };

    private static final String[] EXCLUDED_EXTENSIONS_7X = {
            "org.jboss.as.web",
            "org.jboss.as.messaging",
            "org.jboss.as.threads"
    };

    public static final Set<String> EXTENSIONS_SET_6X = new HashSet<>(Arrays.asList(EXCLUDED_EXTENSIONS_6X));
    public static final Set<String> EXTENSIONS_SET_7X = new HashSet<>(Arrays.asList(EXCLUDED_EXTENSIONS_7X));

    private static final PathElement HOST = PathElement.pathElement("host", "slave");
    private static final PathAddress HOST_EXCLUDE = PathAddress.pathAddress("host-exclude", "test");
    private static final PathElement SOCKET = PathElement.pathElement(SOCKET_BINDING, "http");
    private static final PathAddress CLONE_PROFILE = PathAddress.pathAddress(PROFILE, CLONE);

    private static DomainTestSupport testSupport;

    private static Version.AsVersion version;

    /** Subclasses call from a @BeforeClass method */
    protected static void setup(Class<?> clazz, String hostRelease, ModelVersion slaveApiVersion) throws IOException, MgmtOperationException, TimeoutException, InterruptedException {
        version = clazz.getAnnotation(Version.class).value();

        testSupport = MixedDomainTestSuite.getSupport(clazz);

        if (version.getMajor() == 7) {
            // note that some of these 7+ specific changes may warrant creating a newer version of testing-host.xml for the newer slaves
            // at some point (the currently used host.xml is quite an old version). If these exceptions become more complicated than this, we should
            // probably do that.

            //Unset the ignore-unused-configuration flag
            ModelNode dc = DomainTestUtils.executeForResult(
                    Util.getReadAttributeOperation(PathAddress.pathAddress(HOST), DOMAIN_CONTROLLER),
                    testSupport.getDomainSlaveLifecycleUtil().getDomainClient());

            dc = dc.get("remote");

            dc.get(IGNORE_UNUSED_CONFIG).set(false);
            dc.get(OP).set("write-remote-domain-controller");
            dc.get(OP_ADDR).set(PathAddress.pathAddress(HOST).toModelNode());

            DomainTestUtils.executeForResult(dc, testSupport.getDomainSlaveLifecycleUtil().getDomainClient());
        }

        stopSlave();

        // restarting the slave will recopy the testing-host.xml file over the top, clobbering the ignore-unused-configuration above,
        // so use setRewriteConfigFiles(false) to prevent this.
        WildFlyManagedConfiguration slaveCfg = testSupport.getDomainSlaveConfiguration();
        slaveCfg.setRewriteConfigFiles(false);

        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        setupExclude(client, hostRelease, slaveApiVersion);

        // Add some ignored extensions to verify they are ignored
        addExtensions(true, client);

        startSlave();
    }

    private static void stopSlave() throws IOException, MgmtOperationException, InterruptedException {
        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        executeForResult(Util.createEmptyOperation(SHUTDOWN, PathAddress.pathAddress(HOST)), client);
        boolean gone = false;
        long timeout = TimeoutUtil.adjust(30000);
        long deadline = System.currentTimeMillis() + timeout;
        do {
            ModelNode hosts = readChildrenNames(client, PathAddress.EMPTY_ADDRESS, HOST.getKey());
            gone = true;
            for (ModelNode host : hosts.asList()) {
                if (HOST.getValue().equals(host.asString())) {
                    gone = false;
                    Thread.sleep(100);
                    break;
                }
            }
        } while (!gone && System.currentTimeMillis() < deadline);
        Assert.assertTrue("Slave was not removed within " + timeout + " ms", gone);
        testSupport.getDomainSlaveLifecycleUtil().stop();
    }

    private static void setupExclude(ModelControllerClient client, String hostRelease, ModelVersion hostVersion) throws IOException, MgmtOperationException {

        ModelNode addOp = Util.createAddOperation(HOST_EXCLUDE);
        if (hostRelease != null) {
            addOp.get("host-release").set(hostRelease);
        } else {
            addOp.get("management-major-version").set(hostVersion.getMajor());
            addOp.get("management-minor-version").set(hostVersion.getMinor());
            if (hostVersion.getMicro() != 0) {
                addOp.get("management-micro-version").set(hostVersion.getMicro());
            }
        }
        addOp.get("active-server-groups").add("other-server-group");

        ModelNode asbgs = addOp.get("active-socket-binding-groups");
        asbgs.add("full-sockets");
        asbgs.add("full-ha-sockets");

        ModelNode extensions = addOp.get("excluded-extensions");
        for (String ext : getExcludedExtensions()) {
            extensions.add(ext);
        }

        executeForResult(addOp, client);
    }

    private static void addExtensions(boolean evens, ModelControllerClient client) throws IOException, MgmtOperationException {
        for (int i = 0; i < getExcludedExtensions().length; i++) {
            if ((i % 2 == 0) == evens) {
                executeForResult(Util.createAddOperation(PathAddress.pathAddress(EXTENSION, getExcludedExtensions()[i])), client);
            }
        }
    }

    private static void startSlave() throws TimeoutException, InterruptedException {

        DomainLifecycleUtil legacyUtil = testSupport.getDomainSlaveLifecycleUtil();
        long start = System.currentTimeMillis();
        legacyUtil.start();
        legacyUtil.awaitServers(start);

    }

    @AfterClass
    public static void tearDown() throws IOException, MgmtOperationException, TimeoutException, InterruptedException {
        try {
            executeForResult(createRemoveOperation(HOST_EXCLUDE), testSupport.getDomainMasterLifecycleUtil().getDomainClient());
        } finally {
            restoreSlave();
        }
    }


    @Test
    public void test001SlaveBoot() throws Exception {

        ModelControllerClient slaveClient = testSupport.getDomainSlaveLifecycleUtil().getDomainClient();

        checkExtensions(slaveClient);
        checkProfiles(slaveClient);
        checkSocketBindingGroups(slaveClient);

        checkSockets(slaveClient, PathAddress.pathAddress(SOCKET_BINDING_GROUP, "full-sockets"));
        checkSockets(slaveClient, PathAddress.pathAddress(SOCKET_BINDING_GROUP, "full-ha-sockets"));
    }

    @Test
    public void test002ServerBoot() throws IOException, MgmtOperationException, InterruptedException, OperationFailedException {

        ModelControllerClient masterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        PathAddress serverCfgAddr = PathAddress.pathAddress(HOST,
                PathElement.pathElement(SERVER_CONFIG, "server-one"));
        ModelNode op = Util.createEmptyOperation("start", serverCfgAddr);
        executeForResult(op, masterClient);

        PathAddress serverAddr = PathAddress.pathAddress(HOST,
                PathElement.pathElement(RUNNING_SERVER, "server-one"));
        awaitServerLaunch(masterClient, serverAddr);
        checkSockets(masterClient, serverAddr.append(PathElement.pathElement(SOCKET_BINDING_GROUP, "full-ha-sockets")));

    }

    private void awaitServerLaunch(ModelControllerClient client, PathAddress serverAddr) throws InterruptedException {
        long timeout = TimeoutUtil.adjust(20000);
        long expired = System.currentTimeMillis() + timeout;
        ModelNode op = Util.getReadAttributeOperation(serverAddr, "server-state");
        do {
            try {
                ModelNode state = DomainTestUtils.executeForResult(op, client);
                if ("running".equalsIgnoreCase(state.asString())) {
                    return;
                }
            } catch (IOException | MgmtOperationException e) {
                // ignore and try again
            }

            TimeUnit.MILLISECONDS.sleep(250L);
        } while (System.currentTimeMillis() < expired);

        Assert.fail("Server did not start in " + timeout + " ms");
    }

    @Test
    public void test003PostBootUpdates() throws IOException, MgmtOperationException {

        ModelControllerClient masterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        ModelControllerClient slaveClient = testSupport.getDomainSlaveLifecycleUtil().getDomainClient();

        // Tweak an ignored profile and socket-binding-group to prove slave doesn't see it
        updateExcludedProfile(masterClient);
        updateExcludedSocketBindingGroup(masterClient);

        // Verify profile cloning is ignored when the cloned profile is excluded
        testProfileCloning(masterClient, slaveClient);

        // Add more ignored extensions to verify slave doesn't see the ops
        addExtensions(false, masterClient);
        checkExtensions(slaveClient);

    }

    private void checkExtensions(ModelControllerClient client) throws IOException, MgmtOperationException {
        ModelNode op = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(CHILD_TYPE).set(EXTENSION);
        ModelNode result = executeForResult(op, client);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asInt() > 0);
        for (ModelNode ext : result.asList()) {
            Assert.assertFalse(ext.asString(), getExtensionsSet().contains(ext.asString()));
        }
    }

    private void checkProfiles(ModelControllerClient client) throws IOException, MgmtOperationException {
        ModelNode result = readChildrenNames(client, PathAddress.EMPTY_ADDRESS, PROFILE);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(result.toString(), 1, result.asInt());
        Assert.assertEquals(result.toString(), "full-ha", result.get(0).asString());
    }

    private void checkSocketBindingGroups(ModelControllerClient client) throws IOException, MgmtOperationException {
        ModelNode result = readChildrenNames(client, PathAddress.EMPTY_ADDRESS, SOCKET_BINDING_GROUP);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(result.toString(), 2, result.asInt());
        Set<String> expected = new HashSet<>(Arrays.asList("full-sockets", "full-ha-sockets"));
        for (ModelNode sbg : result.asList()) {
            expected.remove(sbg.asString());
        }
        Assert.assertTrue(result.toString(), expected.isEmpty());
    }

    private void checkSockets(ModelControllerClient client, PathAddress baseAddress) throws IOException, MgmtOperationException, OperationFailedException {
        ModelNode result = readChildrenNames(client, baseAddress, SOCKET_BINDING);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.toString(), result.asInt() > 1);

        ModelNode op = Util.getReadAttributeOperation(baseAddress.append(SOCKET), PORT);
        result = executeForResult(op, client);
        Assert.assertTrue(result.isDefined());

        result = new TestExpressionResolver().resolveExpressions(result);

        Assert.assertEquals(result.toString(), 8080, result.asInt());
    }

    private void updateExcludedProfile(ModelControllerClient client) throws IOException, MgmtOperationException {
        ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, "jmx")), "non-core-mbean-sensitivity", false);
        executeForResult(op, client);
    }

    private void updateExcludedSocketBindingGroup(ModelControllerClient client) throws IOException, MgmtOperationException {
        ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "standard-sockets"),
                PathElement.pathElement(SOCKET_BINDING, "http")), PORT, 8080);
        executeForResult(op, client);
    }

    private static ModelNode readChildrenNames(ModelControllerClient client, PathAddress pathAddress, String childType) throws IOException, MgmtOperationException {
        ModelNode op = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, pathAddress);
        op.get(CHILD_TYPE).set(childType);
        return executeForResult(op, client);
    }

    private void testProfileCloning(ModelControllerClient masterClient, ModelControllerClient slaveClient) throws IOException, MgmtOperationException {
        ModelNode profiles = readChildrenNames(masterClient, PathAddress.EMPTY_ADDRESS, PROFILE);
        Assert.assertTrue(profiles.isDefined());
        Assert.assertTrue(profiles.toString(), profiles.asInt() > 0);

        for (ModelNode mn : profiles.asList()) {
            String profile = mn.asString();
            cloneProfile(masterClient, profile);
            try {
                checkProfiles(slaveClient);
            } finally {
                executeForResult(Util.createRemoveOperation(CLONE_PROFILE), masterClient);
            }
        }
    }

    private void cloneProfile(ModelControllerClient client, String toClone) throws IOException, MgmtOperationException {
        ModelNode op = Util.createEmptyOperation(CLONE, PathAddress.pathAddress(PROFILE, toClone));
        op.get(TO_PROFILE).set(CLONE);
        executeForResult(op, client);
    }

    private static void restoreSlave() throws TimeoutException, InterruptedException {
        DomainLifecycleUtil slaveUtil = testSupport.getDomainSlaveLifecycleUtil();
        if (!slaveUtil.isHostControllerStarted()) {
            startSlave();
        }
    }

    private Set<String> getExtensionsSet() {
        if (version.getMajor() == 6) {
            return EXTENSIONS_SET_6X;
        } else if (version.getMajor() == 7) {
            return EXTENSIONS_SET_7X;
        }
        throw new IllegalStateException("Unknown version " + version);
    }

    private static String[] getExcludedExtensions() {
        if (version.getMajor() == 6) {
            return EXCLUDED_EXTENSIONS_6X;
        } else if (version.getMajor() == 7) {
            return EXCLUDED_EXTENSIONS_7X;
        }
        throw new IllegalStateException("Unknown version " + version);
    }

    private static class TestExpressionResolver extends ExpressionResolverImpl {
        public TestExpressionResolver() {
        }
    }
}
