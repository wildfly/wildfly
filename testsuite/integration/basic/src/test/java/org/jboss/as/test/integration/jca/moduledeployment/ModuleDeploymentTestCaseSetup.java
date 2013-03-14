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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.xnio.IoUtils;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ModuleDeploymentTestCaseSetup extends JcaMgmtServerSetupTask {

    protected File testModuleRoot;
    protected File slot;
    public static ModelNode address;
    protected final String defaultPath = "org/jboss/ironjacamar/ra16out";

    public void addModule(final String moduleName) throws Exception {
        addModule(moduleName, "module.xml");
    }

    public void addModule(final String moduleName, String moduleXml)
            throws Exception {
        removeModule(moduleName);
        createTestModule(moduleXml);
    }

    public void removeModule(final String moduleName) throws Exception {
        testModuleRoot = new File(getModulePath(), moduleName);
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

    private void createTestModule(String moduleXml) throws IOException {
        if (testModuleRoot.exists()) {
            throw new IllegalArgumentException(testModuleRoot
                    + " already exists");
        }
        File file = new File(testModuleRoot, "main");
        if (!file.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + file);
        }
        slot = file;
        URL url = this.getClass().getResource(moduleXml);
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }
        copyFile(new File(file, "module.xml"), url.openStream());

    }

    protected void copyFile(File target, InputStream src) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(target));
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
                throw new IllegalStateException(
                        "Neither -Dmodule.path nor -Djboss.home were set");
            }
            modulePath = jbossHome + File.separatorChar + "modules";
        } else {
            modulePath = modulePath.split(File.pathSeparator)[0];
        }
        File moduleDir = new File(modulePath);
        if (!moduleDir.exists()) {
            throw new IllegalStateException(
                    "Determined module path does not exist");
        }
        if (!moduleDir.isDirectory()) {
            throw new IllegalStateException(
                    "Determined module path is not a dir");
        }
        return moduleDir;
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId)
            throws Exception {
        takeSnapShot();
        remove(address);
        removeModule(defaultPath);
    }

    @Override
    protected void doSetup(ManagementClient managementClient) throws Exception {

        addModule(defaultPath);

    }

    protected void setConfiguration(String fileName) throws Exception {
        String xml = FileUtils.readFile(this.getClass(), fileName);
        List<ModelNode> operations = xmlToModelOperations(xml,
                Namespace.CURRENT.getUriString(),
                new ResourceAdapterSubsystemParser());
        address = operations.get(1).get("address");
        operations.remove(0);
        for (ModelNode op : operations) {
            executeOperation(op);
        }
        //executeOperation(operationListToCompositeOperation(operations));

    }

    /**
     * Creates module structure for uncompressed RA archive. RA classes are in
     * flat form too
     *
     * @throws Exception
     */
    protected void fillModuleWithFlatClasses(String raFile) throws Exception {

        ResourceAdapterArchive rar = ShrinkWrap
                .create(ResourceAdapterArchive.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage()).addClass(
                javax.jms.MessageListener.class);
        rar.addAsManifestResource(this.getClass().getPackage(), raFile,
                "ra.xml");
        rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");
        jar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");
    }

    /**
     * Creates module structure for uncompressed RA archive.
     * RA classes are packed in .jar archive
     *
     * @throws Exception
     */
    protected void fillModuleWithJar(String raFile) throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap
                .create(ResourceAdapterArchive.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage());
        rar.addAsManifestResource(
                PureJarTestCase.class.getPackage(), raFile, "ra.xml");
        rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");

        copyFile(new File(slot, "ra16out.jar"), jar.as(ZipExporter.class).exportAsInputStream());
    }

    /**
     * Returns basic address
     *
     * @return address
     */
    public static ModelNode getAddress() {
        return address;
    }

}
