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

package org.jboss.as.test.integration.deployment.xml.datasource;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;

/**
 * Test deployment of -ds.xml files as JPA data sources
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DeployedXmlJpaDataSourceTestCase {


    private static ModelControllerClient client;
    private static ServerDeploymentManager manager;
    public static final String TEST_DS_XML = "test-ds.xml";
    public static final String JPA_DEPLOYMENT_NAME = "jpaDeployment";

    @Deployment(name = "runner")
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testDsXmlJpaDeployment.jar")
                .addClasses(DeployedXmlJpaDataSourceTestCase.class, JpaRemote.class)
                .addAsManifestResource(DeployedXmlJpaDataSourceTestCase.class.getPackage(),
                        "MANIFEST.MF", "MANIFEST.MF");
    }

    @Deployment(name = JPA_DEPLOYMENT_NAME, testable = false, managed = false)
    public static Archive<?> deployJpa() {
        return ShrinkWrap.create(JavaArchive.class, JPA_DEPLOYMENT_NAME + ".jar")
                .addClasses(Employee.class, JpaRemoteBean.class,
                        JpaRemote.class)
                .addAsManifestResource(DeployedXmlJpaDataSourceTestCase.class.getPackage(),
                        "MANIFEST.MF", "MANIFEST.MF")
                .addAsManifestResource(DeployedXmlJpaDataSourceTestCase.class.getPackage(),
                        "persistence.xml", "persistence.xml");
    }

    @ArquillianResource
    private InitialContext initialContext;

    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void deployDatasource() throws Throwable {
        try {
            client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
            manager = ServerDeploymentManager.Factory.create(client);
            final String packageName = DeployedXmlJpaDataSourceTestCase.class.getPackage().getName().replace(".", "/");
            final DeploymentPlan plan = manager.newDeploymentPlan().add(DeployedXmlJpaDataSourceTestCase.class.getResource("/" + packageName + "/" + TEST_DS_XML)).andDeploy().build();
            final Future<ServerDeploymentPlanResult> future = manager.execute(plan);
            final ServerDeploymentPlanResult result = future.get(20, TimeUnit.SECONDS);
            final ServerDeploymentActionResult actionResult = result.getDeploymentActionResult(plan.getId());
            if (actionResult != null) {
                if (actionResult.getDeploymentException() != null) {
                    throw actionResult.getDeploymentException();
                }
            }
        } catch (Throwable e) {
            if (client != null) {
                client.close();
            }
        }
    }

    @AfterClass
    public static void undeployDatasource() throws IOException {
        if (client != null) {
            final DeploymentPlan undeployPlan = manager.newDeploymentPlan().undeploy(TEST_DS_XML).andRemoveUndeployed().build();
            manager.execute(undeployPlan);
            client.close();
            client = null;
            manager = null;
        }
    }

    @Test
    public void testJpaUsedWithXMLXaDataSource() throws Throwable {
        deployer.deploy(JPA_DEPLOYMENT_NAME);
        try {
            final JpaRemote remote = (JpaRemote) initialContext.lookup("java:global/" + JPA_DEPLOYMENT_NAME + "/" + "JpaRemoteBean");
            remote.addEmployee("Bob");
            remote.addEmployee("Sue");
            final Set<String> emps = remote.getEmployees();
            Assert.assertEquals(2, emps.size());
            Assert.assertTrue(emps.contains("Bob"));
            Assert.assertTrue(emps.contains("Sue"));
        } finally {
            deployer.undeploy(JPA_DEPLOYMENT_NAME);
        }
    }


}
