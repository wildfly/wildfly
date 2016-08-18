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
package org.jboss.as.test.integration.jca.statistics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Resource adapter statistics testCase
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ResourceAdapterStatisticsTestCase extends JcaStatisticsBase {

    static int jndiCount = 0;
    static int archiveCount = 0;
    static final String pack = "org.jboss.as.test.integration.jca.rar";
    static final String fact = "java:jboss/ConnectionFactory";

    @ArquillianResource
    Deployer deployer;

    @AfterClass
    public static void tearDown() {
        jndiCount = 0;
    }

    @After
    public void closeAndUndeploy() {
        String name;
        while (archiveCount > 0) {
            name = getArchiveName(archiveCount--);
            try {
                deployer.undeploy(name);
            } finally {
                try {
                    removeRa(name);
                } catch (Exception e) {

                }
            }
        }
    }

    private void removeRa(String name) throws Exception {
        remove(getRaAddress(name + ".rar"));
    }

    private ModelNode getRaAddress(String name) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "resource-adapters");
        address.add("resource-adapter", name);
        address.protect();
        return address;
    }

    private String getArchiveName(int count) {
        return "archive" + count;
    }

    private ModelNode prepareTest(boolean doubleCF) throws Exception {
        String archiveName = getArchiveName(++archiveCount);
        ModelNode address = getRaAddress(archiveName + ".rar");
        ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("archive").set(archiveName + ".rar");
        executeOperation(operation);

        int howMany = doubleCF ? 2 : 1;
        for (int i = 1; i <= howMany; i++) {
            ModelNode addressConn = address.clone();
            String jndiName = getJndiName(++jndiCount);
            addressConn.add("connection-definitions", jndiName);
            ModelNode operationConn = new ModelNode();
            operationConn.get(OP).set("add");
            operationConn.get(OP_ADDR).set(addressConn);
            operationConn.get("class-name").set(pack + ".MultipleManagedConnectionFactory" + i);
            operationConn.get("jndi-name").set(jndiName);
            executeOperation(operationConn);
            operation = addressConn;
        }
        deployer.deploy(archiveName);
        return operation;

    }

    public static ResourceAdapterArchive createDeployment(String deploymentName) throws Exception {

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, deploymentName + ".jar");
        ja.addPackage(pack);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(ResourceAdapterStatisticsTestCase.class.getPackage(), "ra.xml", "ra.xml");

        return raa;
    }

    @Deployment(name = "archive1", managed = false)
    public static ResourceAdapterArchive first() throws Exception {
        return createDeployment("archive1");
    }

    @Deployment(name = "archive2", managed = false)
    public static ResourceAdapterArchive second() throws Exception {
        return createDeployment("archive2");
    }

    private String getJndiName(int i) {
        return fact + i;
    }

    @Test
    public void testOneConnection() throws Exception {
        ModelNode mn = prepareTest(false);
        testStatistics(mn);
        testStatisticsDouble(mn);
    }

    @Test
    public void testTwoConnections() throws Exception {
        ModelNode mn = prepareTest(false);
        ModelNode mn1 = prepareTest(false);
        testStatistics(mn);
        testStatisticsDouble(mn);
        testStatistics(mn1);
        testStatisticsDouble(mn1);
        testInterference(mn, mn1);
        testInterference(mn1, mn);
    }

    @Test
    public void testTwoConnectionsInOneRa() throws Exception {
        ModelNode mn = prepareTest(true);
        ModelNode mn1 = getAnotherConnection(mn);
        testStatistics(mn);
        testStatisticsDouble(mn);
        testStatistics(mn1);
        testStatisticsDouble(mn1);
        testInterference(mn, mn1);
        testInterference(mn1, mn);
    }

    @Test
    public void testTwoConnectionsInOneRaPlusOneInOther() throws Exception {
        ModelNode mn = prepareTest(true);
        ModelNode mn1 = getAnotherConnection(mn);
        ModelNode mn2 = prepareTest(false);
        testStatistics(mn);
        testStatisticsDouble(mn);
        testStatistics(mn1);
        testStatisticsDouble(mn1);
        testStatistics(mn2);
        testStatisticsDouble(mn2);
        testInterference(mn, mn2);
        testInterference(mn2, mn);
        testInterference(mn2, mn1);
        testInterference(mn1, mn2);
    }

    private ModelNode getAnotherConnection(ModelNode mn) {
        ModelNode another = mn.clone();
        String newValue = mn.get(2).get("connection-definitions").asString();
        int n = 0;
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(newValue);
        while (m.find()) {
            n = Integer.parseInt(m.group());
        }
        --n;
        another.get(2).get("connection-definitions").set(fact + n);
        return another;
    }

    @Override
    public ModelNode translateFromConnectionToStatistics(ModelNode connectionNode) {
        ModelNode statNode = new ModelNode();
        //statNode.add(DEPLOYMENT, connectionNode.get(1).get("resource-adapter").asString());
        statNode.add(SUBSYSTEM, "resource-adapters");
        statNode.add(connectionNode.get(1));
        statNode.add("connection-definitions", connectionNode.get(2).get("connection-definitions").asString());
        statNode.add("statistics", "pool");
        return statNode;
    }
}
