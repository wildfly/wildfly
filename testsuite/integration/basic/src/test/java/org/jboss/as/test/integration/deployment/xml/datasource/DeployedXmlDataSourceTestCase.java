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

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test deployment of -ds.xml files
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(DeployedXmlDataSourceTestCase.DeployedXmlDataSourceTestCaseSetup.class)
public class DeployedXmlDataSourceTestCase {

    static class DeployedXmlDataSourceTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
            final String packageName = DeployedXmlDataSourceTestCase.class.getPackage().getName().replace(".", "/");
            final DeploymentPlan plan = manager.newDeploymentPlan().add(DeployedXmlDataSourceTestCase.class.getResource("/" + packageName + "/" + TEST_DS_XML)).andDeploy().build();
            final Future<ServerDeploymentPlanResult> future = manager.execute(plan);
            final ServerDeploymentPlanResult result = future.get(20, TimeUnit.SECONDS);
            final ServerDeploymentActionResult actionResult = result.getDeploymentActionResult(plan.getId());
            if (actionResult != null) {
                final Throwable deploymentException = actionResult.getDeploymentException();
                if (deploymentException != null) {
                    throw new  RuntimeException(deploymentException);
                }
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
            final DeploymentPlan undeployPlan = manager.newDeploymentPlan().undeploy(TEST_DS_XML).andRemoveUndeployed().build();
            manager.execute(undeployPlan).get();
        }
    }

    public static final String TEST_DS_XML = "test-ds.xml";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testDsXmlDeployment.jar")
                .addClass(DeployedXmlDataSourceTestCase.class)
                .addAsManifestResource(DeployedXmlDataSourceTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
    }

    @ArquillianResource
    private InitialContext initialContext;

    @Test
    public void testDeployedDataSource() throws Throwable {
        final DataSource dataSource = (DataSource) initialContext.lookup("java:jboss/datasources/DeployedDS");
        Assert.assertNotNull(dataSource);
        Connection conn = dataSource.getConnection();
        ResultSet rs = conn.prepareStatement("select 1").executeQuery();
        Assert.assertTrue(rs.next());
        conn.close();
    }


    @Test
    public void testDeployedXaDataSource() throws Throwable {
        final DataSource dataSource = (DataSource) initialContext.lookup("java:/H2XADS");
        Assert.assertNotNull(dataSource);
        Connection conn = dataSource.getConnection();
        ResultSet rs = conn.prepareStatement("select 1").executeQuery();
        Assert.assertTrue(rs.next());
        conn.close();
    }


}
