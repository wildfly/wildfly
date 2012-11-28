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
package org.jboss.as.test.integration.jca.moduledeployment;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import javax.annotation.Resource;
import javax.resource.cci.ConnectionFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;

/**
 * JBQA-6277 -IronJacamar deployments subsystem test case
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ModuleIJDeploymentTestCase.ModuleIJDeploymentTestCaseSetup.class)
public class ModuleIJDeploymentTestCase extends ContainerResourceMgmtTestBase {


    static class ModuleIJDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask {


        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {
            addModule("org/jboss/ironjacamar/ra16outij2", "ra16outij2.rar");
            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", "ra16outij2");
            address.protect();

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("add");
            operation.get(OP_ADDR).set(address);
            operation.get("module").set("org.jboss.ironjacamar.ra16outij2");
            executeOperation(operation);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode address = new ModelNode();
                    address.add("subsystem", "resource-adapters");
                    address.add("resource-adapter", "ra16outij2");
                    address.protect();

                    final ModelNode operation = new ModelNode();
                    operation.get(OP).set("remove");
                    operation.get(OP_ADDR).set(address);
                    executeOperation(operation);
            removeModule("org/jboss/ironjacamar/ra16outij2");
        }

        public void addModule(final String moduleName, final String mainResource) throws Exception {
                File testModuleRoot = new File(getModulePath(), moduleName);
                deleteRecursively(testModuleRoot);
                createTestModule(testModuleRoot, mainResource);
            }

            public void removeModule(final String moduleName) throws Exception {
                File testModuleRoot = new File(getModulePath(), moduleName);
                deleteRecursively(testModuleRoot);
            }

            private void deleteRecursively(File file) {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        for (String name : file.list()) {
                            deleteRecursively(new File(file, name));
                        }
                    }
                    file.delete();
                }
            }

            private void createTestModule(File testModuleRoot, final String mainResource) throws IOException {
                if (testModuleRoot.exists()) {
                    throw new IllegalArgumentException(testModuleRoot + " already exists");
                }
                File file = new File(testModuleRoot, "main");
                if (!file.mkdirs()) {
                    throw new IllegalArgumentException("Could not create " + file);
                }


                URL url = this.getClass().getResource("module.xml");
                if (url == null) {
                    throw new IllegalStateException("Could not find module.xml");
                }
                copyFile(new File(file, "module.xml"), url.openStream());

                url = this.getClass().getResource(mainResource);
                if (url == null) {
                    throw new IllegalStateException("Could not find ra16outij2.rar");
                }
                copyFile(new File(file, "ra16outij2.rar"), url.openStream());

            }

            private void copyFile(File target, InputStream src) throws IOException {
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
                try {
                    int i = src.read();
                    while (i != -1) {
                        out.write(i);
                        i = src.read();
                    }
                } finally {
                    IoUtils.safeClose(out);
                }
            }

            private File getModulePath() {
                String modulePath = System.getProperty("module.path", null);
                if (modulePath == null) {
                    String jbossHome = System.getProperty("jboss.home", null);
                    if (jbossHome == null) {
                        throw new IllegalStateException("Neither -Dmodule.path nor -Djboss.home were set");
                    }
                    modulePath = jbossHome + File.separatorChar + "modules";
                } else {
                    modulePath = modulePath.split(File.pathSeparator)[0];
                }
                File moduleDir = new File(modulePath);
                if (!moduleDir.exists()) {
                    throw new IllegalStateException("Determined module path does not exist");
                }
                if (!moduleDir.isDirectory()) {
                    throw new IllegalStateException("Determined module path is not a dir");
                }
                return moduleDir;
            }
    }


    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static JavaArchive createDeployment() throws Exception {

        String deploymentName = "basic.jar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addClasses(ModuleIJDeploymentTestCase.class, MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class,
                ModuleIJDeploymentTestCaseSetup.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        ja.addAsManifestResource(ModuleIJDeploymentTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"), "MANIFEST.MF");
        return ja;
    }

    @Resource(mappedName = "java:/testMe2")
    private ConnectionFactory connectionFactory;

    /**
     * Test configuration - if all properties propagated to the model
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        assertNotNull(connectionFactory);
    }


}
