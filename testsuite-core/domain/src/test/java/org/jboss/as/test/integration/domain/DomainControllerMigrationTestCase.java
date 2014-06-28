/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.deployment.trivial.ServiceActivatorDeploymentUtil;
import org.jboss.as.test.integration.domain.management.util.DomainControllerClientConfig;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.sasl.util.UsernamePasswordHashUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of migration of the domain controller from one host to another
 *
 * @author <a href="dpospisi@redhat.com">Dominik Pospisil</a>
 */
public class DomainControllerMigrationTestCase {

    private static final Logger log = Logger.getLogger(DomainControllerMigrationTestCase.class);

    private static String[] SERVERS = new String[] {"failover-one", "failover-two", "failover-three"};
    private static String[] HOSTS = new String[] {"failover-h1", "failover-h2", "failover-h3"};
    private static int[] MGMT_PORTS = new int[] {9999, 9989, 19999};
    private static String masterAddress = System.getProperty("jboss.test.host.master.address");
    private static String slaveAddress = System.getProperty("jboss.test.host.slave.address");
    private static final String DEPLOYMENT_NAME = "service-activator.jar";

    private static DomainControllerClientConfig domainControllerClientConfig;
    private static DomainLifecycleUtil[] hostUtils = new DomainLifecycleUtil[3];

    private static File jarFile;

    @BeforeClass
    public static void setupDomain() throws Exception {
        domainControllerClientConfig = DomainControllerClientConfig.create();
        for (int k = 0; k<3; k++) {
            hostUtils[k] = new DomainLifecycleUtil(getHostConfiguration(k+1), domainControllerClientConfig);
            hostUtils[k].start();
        }

        String tempDir = System.getProperty("java.io.tmpdir");
        jarFile = new File(tempDir + File.separator + DEPLOYMENT_NAME);
        ServiceActivatorDeploymentUtil.createServiceActivatorDeployment(jarFile);

    }

    @AfterClass
    public static void shutdownDomain() throws IOException {
        try {
            int i = 0;
            for (; i < hostUtils.length; i++) {
                try {
                    hostUtils[i].stop();
                } catch (Exception e) {
                    log.error("Failed closing host util " + i, e);
                }
            }
        } finally {
            if (domainControllerClientConfig != null) {
                domainControllerClientConfig.close();
            }
        }

        Assert.assertTrue(jarFile.delete());
    }

    private static WildFlyManagedConfiguration getHostConfiguration(int host) throws Exception {

        final String testName = DomainControllerMigrationTestCase.class.getSimpleName();
        File domains = new File("target" + File.separator + "domains" + File.separator + testName);
        final File hostDir = new File(domains, "failover" + String.valueOf(host));
        final File hostConfigDir = new File(hostDir, "configuration");
        assert hostConfigDir.mkdirs() || hostConfigDir.isDirectory();

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final WildFlyManagedConfiguration hostConfig = new WildFlyManagedConfiguration();
        hostConfig.setHostControllerManagementAddress(host == 1 ? masterAddress : slaveAddress);
        hostConfig.setHostCommandLineProperties("-Djboss.test.host.master.address=" + masterAddress + " -Djboss.test.host.slave.address=" + slaveAddress);
        URL url = tccl.getResource("domain-configs/domain-standard.xml");
        assert url != null;
        hostConfig.setDomainConfigFile(new File(url.toURI()).getAbsolutePath());
        System.out.println(hostConfig.getDomainConfigFile());
        url = tccl.getResource("host-configs/host-failover" + String.valueOf(host) + ".xml");
        assert url != null;
        hostConfig.setHostConfigFile(new File(url.toURI()).getAbsolutePath());
        System.out.println(hostConfig.getHostConfigFile());
        hostConfig.setDomainDirectory(hostDir.getAbsolutePath());
        hostConfig.setHostName("failover-h" + String.valueOf(host));
        hostConfig.setHostControllerManagementPort(MGMT_PORTS[host - 1]);
        hostConfig.setStartupTimeoutInSeconds(120);
        hostConfig.setBackupDC(true);
        File usersFile = new File(hostConfigDir, "mgmt-users.properties");
        FileOutputStream fos = new FileOutputStream(usersFile);
        PrintWriter pw = new PrintWriter(fos);
        pw.println("slave=" + new UsernamePasswordHashUtil().generateHashedHexURP("slave", "ManagementRealm", "slave_user_password".toCharArray()));
        pw.close();
        fos.close();


        return hostConfig;
    }

    @Test
    /**
     * Test setup: 3 hosts which each having exactly one server defined - failover-h1, failover-h2, failover-h3
     * Test scenario:
     * 1) kill failover-h1 host controller
     * 2) update failover-h2 configuration to act as the domain controller
     * 3) reload failover-h2 without restarting server
     * 4) update failover-h3 config to point to failover-h2 as a new domain controller
     * 5) reload failover-h3
     * 6) verify that failover-h2 is the new domain controller managing both failover-h2 and failover-h3 hosts
     */
    public void testDCFailover() throws Exception {

        // check that the failover-h1 is acting as domain controller and all three servers are registered
        Set<String> hosts = getHosts(hostUtils[0]);
        Assert.assertTrue(hosts.contains(HOSTS[0]));
        Assert.assertTrue(hosts.contains(HOSTS[1]));
        Assert.assertTrue(hosts.contains(HOSTS[2]));

        // Add a system property so we can prove it is still there after failover
        ModelNode addSystemProp = new ModelNode();
        addSystemProp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SYSTEM_PROPERTY, "hc-failover-a");
        addSystemProp.get(ModelDescriptionConstants.OP).set("add");
        addSystemProp.get(ModelDescriptionConstants.VALUE).set("test-a");
        hostUtils[0].executeForResult(addSystemProp);

        // WFLY-3418 Add but don't map a deployment so we can prove the backup gets the content
        Operation addDeployOp = buildDeploymentAddOperation();
        hostUtils[0].executeForResult(addDeployOp);
        assertSlaveHostInfo(hostUtils[0].getDomainClient());

        // kill the domain process controller on failover-h1
        log.info("Stopping the domain controller on failover-h1.");
        hostUtils[0].stop();
        log.info("Domain controller on failover-h1 stopped.");

        // check that the failover-h2 hc sees only itself
        hosts = getHosts(hostUtils[1]);
        Assert.assertTrue(hosts.contains(HOSTS[1]));
        Assert.assertEquals(hosts.size(), 1);

        // Reconfigure failover-h2 host so it acts as domain controller
        ModelNode becomeMasterOp = new ModelNode();
        becomeMasterOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, "failover-h2");
        becomeMasterOp.get(ModelDescriptionConstants.OP).set("write-local-domain-controller");

        hostUtils[1].executeForResult(becomeMasterOp);

        ModelNode restartOp = new ModelNode();
        restartOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, "failover-h2");
        restartOp.get(ModelDescriptionConstants.OP).set("reload");
        restartOp.get(ModelDescriptionConstants.RESTART_SERVERS).set(false);

        log.info("Reloading failover-h2 to act as the domain controller.");
        hostUtils[1].executeAwaitConnectionClosed(restartOp);

        waitUntilHostControllerReady(hostUtils[1]);

        // Read the first system property. This proves we are using the config provided via failover-h1
        ModelNode readSysPropOp = new ModelNode();
        readSysPropOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SYSTEM_PROPERTY, "hc-failover-a");
        readSysPropOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        readSysPropOp.get(ModelDescriptionConstants.NAME).set(ModelDescriptionConstants.VALUE);
        Assert.assertEquals("test-a", hostUtils[1].executeForResult(readSysPropOp).asString());

        // Add a 2nd system property so we can prove it is still there after failover-h3 connects
        addSystemProp = new ModelNode();
        addSystemProp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SYSTEM_PROPERTY, "hc-failover-b");
        addSystemProp.get(ModelDescriptionConstants.OP).set("add");
        addSystemProp.get(ModelDescriptionConstants.VALUE).set("test-b");
        hostUtils[1].executeForResult(addSystemProp);

        // Reconfigure failover-h3 to point to the new domain controller
        ModelNode changeMasterOp = new ModelNode();
        changeMasterOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, HOSTS[2]);
        changeMasterOp.get(ModelDescriptionConstants.OP).set("write-remote-domain-controller");
        changeMasterOp.get(ModelDescriptionConstants.HOST).set("${jboss.test.host.slave.address}");
        changeMasterOp.get(ModelDescriptionConstants.PORT).set(MGMT_PORTS[1]);
        changeMasterOp.get(ModelDescriptionConstants.SECURITY_REALM).set("ManagementRealm");

        hostUtils[2].executeForResult(changeMasterOp);

        restartOp = new ModelNode();
        restartOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, HOSTS[2]);
        restartOp.get(ModelDescriptionConstants.OP).set("reload");
        restartOp.get(ModelDescriptionConstants.RESTART_SERVERS).set(false);

        log.info("Reloading failover-h3 to register to new domain controller.");
        hostUtils[2].executeAwaitConnectionClosed(restartOp);

        waitUntilHostControllerReady(hostUtils[2]);

        // Read the second system property. This proves we correctly got the config from failover-h2.
        readSysPropOp = new ModelNode();
        readSysPropOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SYSTEM_PROPERTY, "hc-failover-b");
        readSysPropOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        readSysPropOp.get(ModelDescriptionConstants.NAME).set(ModelDescriptionConstants.VALUE);
        Assert.assertEquals("test-b", hostUtils[2].executeForResult(readSysPropOp).asString());

        // test some management ops on failover-h3 using new domain controller on failover-h2

        hosts = getHosts(hostUtils[1]);
        Assert.assertTrue(hosts.contains(HOSTS[1]));
        Assert.assertTrue(hosts.contains(HOSTS[2]));
        Assert.assertEquals(hosts.size(), 2);

        // WFLY-3418 -- map deployment to a server group and validate it gets installed on servers
        Operation mapDeployOp = buildDeploymentMappingOperation();
        hostUtils[1].executeForResult(mapDeployOp);
        checkProperty(hostUtils[1].getDomainClient(), HOSTS[1], SERVERS[1]);
        checkProperty(hostUtils[1].getDomainClient(), HOSTS[2], SERVERS[2]);

        // stop failover-h3 server using new dc
        ModelNode stopOp = new ModelNode();
        stopOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, HOSTS[2]);
        stopOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, SERVERS[2]);
        stopOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.STOP);
        hostUtils[1].executeForResult(stopOp);
        DomainTestUtils.waitUntilState(hostUtils[1].getDomainClient(), HOSTS[2], SERVERS[2], "STOPPED");
    }

    private Set<String> getHosts(DomainLifecycleUtil hostUtil) throws IOException {
        ModelNode readOp = new ModelNode();
        readOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        ModelNode domain = hostUtil.executeForResult(readOp);
        Assert.assertTrue(domain.get("host").isDefined());
        return domain.get("host").keys();
    }

    private void waitUntilHostControllerReady(final DomainLifecycleUtil dcUtil) throws TimeoutException {
        long now = System.currentTimeMillis();
        try {
            dcUtil.awaitHostController(now);
            dcUtil.awaitServers(now);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private Operation buildDeploymentAddOperation() throws IOException, OperationFormatException {

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, DEPLOYMENT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addDeploymentOp.get("content").get(0).get("input-stream-index").set(0);

        OperationBuilder ob = new OperationBuilder(addDeploymentOp, true);
        ob.addInputStream(new FileInputStream(jarFile));

        return ob.build();
    }

    private Operation buildDeploymentMappingOperation() throws IOException, OperationFormatException {

        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);

        OperationBuilder ob = new OperationBuilder(deployOp, true);
        ob.addInputStream(new FileInputStream(jarFile));

        return ob.build();
    }

    private void checkProperty(ModelControllerClient client, String host, String server) throws IOException, MgmtOperationException {
        PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, host),
                PathElement.pathElement(SERVER, server));
        ServiceActivatorDeploymentUtil.validateProperties(client, pathAddress);
    }

    private void assertSlaveHostInfo(final ModelControllerClient client) throws Exception {

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR)
                .add(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT)
                .add(ModelDescriptionConstants.HOST_CONNECTION, "*");
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);

        final ModelNode result = DomainTestUtils.executeForResult(operation, client);
        System.out.println(result);
        int results = 0;
        for (final ModelNode node : result.asList()) {
            results++;
            for (final ModelNode event : node.get("result", "events").asList()) {
                Assert.assertTrue(event.hasDefined("type"));
                Assert.assertTrue(event.hasDefined("address"));
                Assert.assertTrue(event.hasDefined("timestamp"));
            }
        }
        Assert.assertEquals(3, results);
    }

}
