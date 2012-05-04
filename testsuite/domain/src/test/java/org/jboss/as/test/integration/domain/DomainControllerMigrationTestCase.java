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

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.process.ProcessController;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.sasl.util.UsernamePasswordHashUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * FailoverTestCase
 *
 * @author <a href="dpospisi@redhat.com">Dominik Pospisil</a>
 */

@Ignore // Breaks IPv6
public class DomainControllerMigrationTestCase {
    
    private static final Logger log = Logger.getLogger(DomainControllerMigrationTestCase.class);
    
    private static String[] SERVERS = new String[] {"failover-one", "failover-two", "failover-three"};
    private static String[] HOSTS = new String[] {"failover1", "failover2", "failover3"};
    private static int[] MGMT_PORTS = new int[] {9999, 9989, 9979};
    private static int[] SERVER_PORT_OFFSETS = new int[] {0, 150, 300};
    private static String masterAddress = System.getProperty("jboss.test.host.master.address", "127.0.0.1");
    private static String slaveAddress = System.getProperty("jboss.test.host.slave.address", "127.0.0.1");       
    private static final String DEPLOYMENT_NAME = "SimpleServlet.war";
    
    private ManagementClient failover1client;
    private static DomainLifecycleUtil[] hostUtils = new DomainLifecycleUtil[3];
    
    private static WebArchive war;
    private static File warFile;    
    
    @BeforeClass
    public static void setupDomain() throws Exception {                
        for (int k = 0; k<3; k++) {
            hostUtils[k] = new DomainLifecycleUtil(getHostConfiguration(k+1));
            hostUtils[k].start();
        }
        
        war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version1"), "page.html");
        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir + File.separator + DEPLOYMENT_NAME);
        new ZipExporterImpl(war).exportTo(warFile, true);
        
    }
    
    @AfterClass
    public static void shutdownDomain() {
        hostUtils[1].stop();
        hostUtils[2].stop();
        
        Assert.assertTrue(warFile.delete());        
    }
        
    private static JBossAsManagedConfiguration getHostConfiguration(int host) throws Exception {

        final String testName = DomainControllerMigrationTestCase.class.getSimpleName();        
        File domains = new File("target" + File.separator + "domains" + File.separator + testName);
        final File hostDir = new File(domains, "failover" + String.valueOf(host));
        final String hostDirPath = hostDir.getAbsolutePath();
        final File hostConfigDir = new File(hostDir, "configuration");
        hostConfigDir.mkdirs();        

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final JBossAsManagedConfiguration hostConfig = new JBossAsManagedConfiguration();
        hostConfig.setHostControllerManagementAddress(masterAddress);
        hostConfig.setHostCommandLineProperties("-Djboss.test.host.master.address=" + masterAddress + " -Djboss.test.host.slave.address=" + slaveAddress);
        URL url = tccl.getResource("domain-configs/domain-standard.xml");
        hostConfig.setDomainConfigFile(new File(url.toURI()).getAbsolutePath());
        System.out.println(hostConfig.getDomainConfigFile());
        url = tccl.getResource("host-configs/host-failover" + String.valueOf(host) + ".xml");
        hostConfig.setHostConfigFile(new File(url.toURI()).getAbsolutePath());
        System.out.println(hostConfig.getHostConfigFile());
        hostConfig.setDomainDirectory(hostDir.getAbsolutePath());
        hostConfig.setHostName("failover" + String.valueOf(host));
        hostConfig.setHostControllerManagementPort(MGMT_PORTS[host - 1]);
        hostConfig.setStartupTimeoutInSeconds(120);
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
     * Test setup: 3 hosts which each having exactly one server defined - failover1, failover2, failover3
     * Test scenario:
     * 1) kill failover1 host controller
     * 2) update failover2 configuration to act as the domain controller
     * 3) reload failover2 without restarting server
     * 4) update failover3 config to point to failover2 as a new domain controller
     * 5) reload failover3
     * 6) verify that failover2 is the new domain controller managing both failover2 and failover3 hosts
     */
    public void testDCFailover() throws Exception {
        
        // check that the failover1 is acting as domain controller and all three servers are registered
        Set<String> hosts = getHosts(hostUtils[0]);
        Assert.assertTrue(hosts.contains(HOSTS[0]));
        Assert.assertTrue(hosts.contains(HOSTS[1]));
        Assert.assertTrue(hosts.contains(HOSTS[2]));
        
        // kill the domain process controller on failover1
        log.info("Stopping the domain controller on failover1.");
        hostUtils[0].stop();        
        log.info("Domain controller on failover1 stopped.");
        
        // check that the failover2 hc sees only its own server
        hosts = getHosts(hostUtils[1]);
        Assert.assertTrue(hosts.contains(HOSTS[1]));
        Assert.assertEquals(hosts.size(), 1);
        
        // reload failover2 host so it acts as domain controller
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();        
        URL url = tccl.getResource("host-configs/host-failover2-v2.xml");
        File cfgFile = new File(url.toURI());
        FileUtils.copyFile(cfgFile, new File(getHostConfigDir(2), "testing-host-failover2.xml"));
        
        ModelNode restartOp = new ModelNode();
        restartOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, "failover2");
        restartOp.get(ModelDescriptionConstants.OP).set("reload");
        restartOp.get(ModelDescriptionConstants.RESTART_SERVERS).set(false);
        
        log.info("Reloading failover2 to act as the domain controller.");
        hostUtils[1].executeAwaitConnectionClosed(restartOp);
        
        waitUntilHostControllerReady(hostUtils[1]);
        
        // reload failover3 to point to the new domain controller
        url = tccl.getResource("host-configs/host-failover3-v2.xml");
        cfgFile = new File(url.toURI());
        FileUtils.copyFile(cfgFile, new File(getHostConfigDir(3), "testing-host-failover3.xml"));
        
        restartOp = new ModelNode();
        restartOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, HOSTS[2]);
        restartOp.get(ModelDescriptionConstants.OP).set("reload");
        restartOp.get(ModelDescriptionConstants.RESTART_SERVERS).set(false);

        log.info("Reloading failover3 to register to new domain controller.");
        hostUtils[2].executeAwaitConnectionClosed(restartOp);

        waitUntilHostControllerReady(hostUtils[2]);
        
        // test some management ops on failover3 using new domain controller on failover2
        
        Operation deployOp = buildDeployOperation();
        hostUtils[1].executeForResult(deployOp);        
        
        hosts = getHosts(hostUtils[1]);
        Assert.assertTrue(hosts.contains(HOSTS[1]));
        Assert.assertTrue(hosts.contains(HOSTS[2]));
        Assert.assertEquals(hosts.size(), 2);
        
        // stop failover3 server using new dc
        String testUrl = new URL("http", slaveAddress, 8080 + SERVER_PORT_OFFSETS[2], "/SimpleServlet/SimpleServlet").toString();
        Assert.assertTrue(WebUtil.testHttpURL(testUrl));
        ModelNode stopOp = new ModelNode();
        stopOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.HOST, HOSTS[2]);
        stopOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, SERVERS[2]);
        stopOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.STOP);
        hostUtils[1].executeForResult(stopOp);
        DomainTestUtils.waitUntilState(hostUtils[1].getDomainClient(), HOSTS[2], SERVERS[2], "STOPPED");
        Assert.assertFalse(WebUtil.testHttpURL(testUrl));        
    }

    private Set<String> getHosts(DomainLifecycleUtil hostUtil) throws IOException {
        ModelNode readOp = new ModelNode();
        readOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);        
        ModelNode domain = hostUtil.executeForResult(readOp);
        Assert.assertTrue(domain.get("host").isDefined());
        return domain.get("host").keys();
    }
        
    private File getHostConfigDir(int host) {
        final String testName = DomainControllerMigrationTestCase.class.getSimpleName();        
        File domains = new File("target" + File.separator + "domains" + File.separator + testName);
        final File hostDir = new File(domains, "failover" + String.valueOf(host));
        final String hostDirPath = hostDir.getAbsolutePath();
        final File hostConfigDir = new File(hostDir, "configuration");
        return hostConfigDir;
    }

    private void waitUntilHostControllerReady(final DomainLifecycleUtil dcUtil) throws TimeoutException {
        RetryTaskExecutor<Object> executor = new RetryTaskExecutor<Object>();
        executor.retryTask(new Callable<Object>() {

            public Object call() throws Exception {
                ModelNode readOp = new ModelNode();
                readOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
                return dcUtil.executeForResult(readOp);
            }
        });
        
    }
    
    private Operation buildDeployOperation() throws IOException, OperationFormatException {

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add("deployment", "SimpleServlet.war");
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addDeploymentOp.get("content").get(0).get("input-stream-index").set(0);
        addDeploymentOp.get(ModelDescriptionConstants.RUNTIME_NAME).set(DEPLOYMENT_NAME);

        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(new FileInputStream(warFile));

        return ob.build();
    }
    
}
