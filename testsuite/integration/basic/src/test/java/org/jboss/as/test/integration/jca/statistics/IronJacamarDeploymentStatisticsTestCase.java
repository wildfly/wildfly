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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Resource adapter statistics testCase
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IronJacamarDeploymentStatisticsTestCase extends JcaStatisticsBase {

    static final String pack = "org.jboss.as.test.integration.jca.rar";
    static final String fact = "java:jboss/ConnectionFactory";

    @ArquillianResource
    Deployer deployer;


    public static ResourceAdapterArchive createDeployment(String deploymentName) throws Exception {

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, "archive" + deploymentName + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, deploymentName + ".jar");
        ja.addPackage(pack);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(IronJacamarDeploymentStatisticsTestCase.class.getPackage(), "ra.xml", "ra.xml");
        raa.addAsManifestResource(IronJacamarDeploymentStatisticsTestCase.class.getPackage(), "ironjacamar" + deploymentName + ".xml", "ironjacamar.xml");
        return raa;
    }

    @Deployment(name = "1", managed = false)
    public static ResourceAdapterArchive first() throws Exception {
        return createDeployment("1");
    }

    @Deployment(name = "2", managed = false)
    public static ResourceAdapterArchive second() throws Exception {
        return createDeployment("2");
    }

    @Deployment(name = "3", managed = false)
    public static ResourceAdapterArchive third() throws Exception {
        return createDeployment("3");
    }

    public ModelNode prepareTest(String name) throws Exception {
        ModelNode address = new ModelNode();
        String arch = "archive" + name + ".rar";
        address.add(DEPLOYMENT, arch)
                .add(SUBSYSTEM, "resource-adapters")
                .add("ironjacamar", "ironjacamar")
                .add("resource-adapter", arch)
                .add("connection-definitions", fact + name);

        deployer.deploy(name);

        enableStats(name, fact + name);

        return address;
    }

    private void enableStats(String name, String cdName) throws Exception {
        String arch = "archive" + name + ".rar";
        ModelNode statAddress = new ModelNode();
        statAddress.add(DEPLOYMENT, arch)
                .add(SUBSYSTEM, "resource-adapters")
                .add("ironjacamar", "ironjacamar")
                .add("resource-adapter", arch)
                .add("connection-definitions", cdName)
                .add("statistics", "pool");

        statAddress.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get(OP_ADDR).set(statAddress);
        operation.get("name").set("statistics-enabled");
        operation.get("value").set(true);
        executeOperation(operation);
    }

    @Test
    public void testOneConnection() throws Exception {
        ModelNode mn = prepareTest("1");
        testStatistics(mn);
        testStatisticsDouble(mn);
        deployer.undeploy("1");
    }

    @Test
    public void testTwoConnections() throws Exception {
        ModelNode mn = prepareTest("1");
        ModelNode mn1 = prepareTest("2");
        testStatistics(mn);
        testStatisticsDouble(mn);
        testStatistics(mn1);
        testStatisticsDouble(mn1);
        testInterference(mn, mn1);
        testInterference(mn1, mn);
        deployer.undeploy("2");
        deployer.undeploy("1");
    }

    @Test
    public void testTwoConnectionsInOneRa() throws Exception {
        ModelNode mn = prepareTest("3");
        ModelNode mn1 = getAnotherConnection(mn);
        enableStats("3", mn1.get(4).get("connection-definitions").asString());
        testStatistics(mn);
        testStatisticsDouble(mn);
        testStatistics(mn1);
        testStatisticsDouble(mn1);
        testInterference(mn, mn1);
        testInterference(mn1, mn);
        deployer.undeploy("3");
    }

    @Test
    public void testTwoConnectionsInOneRaPlusOneInOther() throws Exception {
        ModelNode mn = prepareTest("3");
        ModelNode mn1 = getAnotherConnection(mn);
        enableStats("3", mn1.get(4).get("connection-definitions").asString());
        ModelNode mn2 = prepareTest("1");
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
        deployer.undeploy("1");
        deployer.undeploy("3");
    }

    private ModelNode getAnotherConnection(ModelNode mn) {
        ModelNode another = mn.clone();
        String newValue = mn.get(4).get("connection-definitions").asString();
        int n = 0;
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(newValue);
        while (m.find()) {
            n = Integer.parseInt(m.group());
        }
        ++n;
        another.get(4).get("connection-definitions").set(fact + n);
        return another;
    }

    @Override
    public ModelNode translateFromConnectionToStatistics(ModelNode connectionNode) {
        ModelNode statNode = new ModelNode();
        statNode.add(DEPLOYMENT, connectionNode.get(0).get(DEPLOYMENT).asString());
        statNode.add(SUBSYSTEM, "resource-adapters");
        statNode.add("ironjacamar", "ironjacamar");
        statNode.add(connectionNode.get(3));
        statNode.add("connection-definitions", connectionNode.get(4).get("connection-definitions").asString());
        statNode.add("statistics", "pool");
        return statNode;
    }
}
