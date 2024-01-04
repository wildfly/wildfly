/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.repository.ContentFilter;
import org.jboss.as.repository.ContentRepositoryElement;
import org.jboss.as.repository.PathUtil;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.test.integration.deployment.classloading.ear.TestAA;
import org.jboss.as.test.integration.deployment.classloading.ear.TestBB;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.EJBBusinessInterface;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.SimpleSLSB;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.HelloWorldServlet;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
public class DeploymentDuplicationTestCase {

    private static final String EJB_DEPLOYMENT = "ejb.jar";
    private static final String WEB_DEPLOYMENT = "web.war";
    private static final String JAR_DEPLOYMENT_A = "lib-a.jar";
    private static final String JAR_DEPLOYMENT_B = "lib-b.jar";
    private static final String EAR_DEPLOYMENT = "ear.ear";

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    private ContentFilter contentFilter = contentFilter("content");

    @Deployment(name = EJB_DEPLOYMENT, managed = false)
    public static JavaArchive createEJBDeployment() {
        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, EJB_DEPLOYMENT);
        ejb.addClasses(EJBBusinessInterface.class, SimpleSLSB.class);
        return ejb;
    }

    @Deployment(name = WEB_DEPLOYMENT, managed = false)
    public static WebArchive createWebDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_DEPLOYMENT);
        war.addClasses(HelloWorldServlet.class, EJBBusinessInterface.class, SimpleSLSB.class);
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, JAR_DEPLOYMENT_A)
                .addClasses(TestAA.class);
        war.addAsLibrary(libJar);
        return war;
    }

    @Deployment(name = EAR_DEPLOYMENT, managed = false)
    public static EnterpriseArchive createEARDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT);
        ear.addAsModule(createEJBDeployment())
                .addAsModule(createWebDeployment());
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, JAR_DEPLOYMENT_B)
                .addClasses(TestBB.class);
        ear.addAsLibrary(libJar);
        return ear;
    }

    private ContentFilter contentFilter(String fileName) {
        return new ContentFilter() {
            @Override
            public boolean acceptFile(Path rootPath, Path file) throws IOException {
                return file.endsWith(fileName);
            }
            @Override
            public boolean acceptFile(Path rootPath, Path file, InputStream in) throws IOException {
                return file.endsWith(fileName);
            }

            @Override
            public boolean acceptDirectory(Path rootPath, Path path) throws IOException {
                return false;
            }
        };
    }

    @Test
    public void testEjbDeployment() throws Exception {
        try {
            ModelControllerClient mcc = managementClient.getControllerClient();
            deployer.deploy(EJB_DEPLOYMENT);
            Path tempPath = Paths.get(ManagementOperations.executeOperation(mcc, readTempPathOp()).asString(), "vfs", "temp");
            List<ContentRepositoryElement> elements = PathUtil.listFiles(tempPath, null, contentFilter);
            Assert.assertTrue("There should be no content file in the tmp/vfs/temp directory", elements.isEmpty());
        } finally {
            deployer.undeploy(EJB_DEPLOYMENT);
        }
    }

    @Test
    public void testWarDeployment() throws Exception {
        try {
            ModelControllerClient mcc = managementClient.getControllerClient();
            deployer.deploy(WEB_DEPLOYMENT);
            Path tmpDeploymentPath = Paths.get(ManagementOperations.executeOperation(mcc, readTempPathOp()).asString(), "vfs", "deployment");
            List<ContentRepositoryElement> libInDeploymentDir = PathUtil.listFiles(tmpDeploymentPath, null, contentFilter(JAR_DEPLOYMENT_A));
            Assert.assertTrue("There should be no lib-a.jar file in the tmp/vfs/deployment directory", libInDeploymentDir.isEmpty());
        } finally {
            deployer.undeploy(WEB_DEPLOYMENT);
        }
    }

    @Test
    public void testEarDeployment() throws Exception {
        try {
            ModelControllerClient mcc = managementClient.getControllerClient();
            deployer.deploy(EAR_DEPLOYMENT);
            Path tempPath = Paths.get(ManagementOperations.executeOperation(mcc, readTempPathOp()).asString(), "vfs", "temp");
            List<ContentRepositoryElement> elements = PathUtil.listFiles(tempPath, null, contentFilter);
            Assert.assertTrue("There should be no content file in the tmp/vfs/temp directory", elements.isEmpty());
            Path tmpDeploymentPath = Paths.get(ManagementOperations.executeOperation(mcc, readTempPathOp()).asString(), "vfs", "deployment");
            List<ContentRepositoryElement> libAInDeploymentDir = PathUtil.listFiles(tmpDeploymentPath, null, contentFilter(JAR_DEPLOYMENT_A));
            Assert.assertEquals("There should be 1 lib-a.jar file in the tmp/vfs/deployment directory", 1, libAInDeploymentDir.size());
            List<ContentRepositoryElement> libBInDeploymentDir = PathUtil.listFiles(tmpDeploymentPath, null, contentFilter(JAR_DEPLOYMENT_B));
            Assert.assertTrue("There should be no lib-b.jar file in the tmp/vfs/deployment directory", libBInDeploymentDir.isEmpty());
            List<ContentRepositoryElement> libBInTempDir = PathUtil.listFiles(tempPath, null, contentFilter(JAR_DEPLOYMENT_B));
            Assert.assertEquals("There should be 1 lib-b.jar file in the tmp/vfs/temp directory", 1, libBInTempDir.size());
        } finally {
            deployer.undeploy(EAR_DEPLOYMENT);
        }
    }

    private Operation readTempPathOp() {
        final ModelNode readAttributeOperation = Util.getReadAttributeOperation(PathAddress.pathAddress("path", ServerEnvironment.SERVER_TEMP_DIR), "path");
        return Operation.Factory.create(readAttributeOperation, Collections.emptyList(), true);
    }

}
