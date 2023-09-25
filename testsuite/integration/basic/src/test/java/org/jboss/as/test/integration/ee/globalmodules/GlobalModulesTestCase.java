/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.globalmodules;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(GlobalModulesTestCase.GlobalModulesTestCaseServerSetup.class)
public class GlobalModulesTestCase {

    static class GlobalModulesTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode value = new ModelNode();
            ModelNode module = new ModelNode();
            module.get("name").set("org.jboss.test.globalModule");
            module.get("annotations").set(true);
            value.add(module);

            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ee");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("global-modules");
            op.get(VALUE).set(value);
            managementClient.getControllerClient().execute(op);

            File testModuleRoot = new File(getModulePath(), "org/jboss/test/globalModule");
            File file = testModuleRoot;
            while (!getModulePath().equals(file.getParentFile()))
                file = file.getParentFile();
            deleteRecursively(file);
            createTestModule(testModuleRoot);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ee");
            op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("global-modules");
            managementClient.getControllerClient().execute(op);

            File testModuleRoot = new File(getModulePath(), "org/jboss/test/globalModule");
            File file = testModuleRoot;
            while (!getModulePath().equals(file.getParentFile()))
                file = file.getParentFile();
            deleteRecursively(file);
        }

        private static void deleteRecursively(File file) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    for (String name : file.list()) {
                        deleteRecursively(new File(file, name));
                    }
                }
                file.delete();
            }
        }

        private static void createTestModule(File testModuleRoot) throws IOException {
            if (testModuleRoot.exists()) {
                throw new IllegalArgumentException(testModuleRoot + " already exists");
            }
            File file = new File(testModuleRoot, "main");
            if (!file.mkdirs()) {
                throw new IllegalArgumentException("Could not create " + file);
            }

            URL url = GlobalModulesTestCase.class.getResource("module.xml");
            if (url == null) {
                throw new IllegalStateException("Could not find module.xml");
            }
            copyFile(new File(file, "module.xml"), url.openStream());

            JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "globalTest.jar");
            jar.addClasses(GlobalModuleEjb.class, GlobalModuleInterceptor.class);

            final ClassLoader cl = GlobalModulesTestCase.class.getClassLoader();

            //create annotation index
            Indexer indexer = new Indexer();
            indexer.index(cl.getResourceAsStream(GlobalModuleEjb.class.getName().replace(".","/") + ".class"));
            indexer.index(cl.getResourceAsStream(GlobalModuleInterceptor.class.getName().replace(".", "/") + ".class"));
            final Index index = indexer.complete();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            IndexWriter writer = new IndexWriter(out);
            writer.write(index);
            jar.addAsManifestResource(new ByteArrayAsset(out.toByteArray()), "jandex.idx");


            FileOutputStream jarFile = new FileOutputStream(new File(file, "globalTest.jar"));
            try {
                jar.as(ZipExporter.class).exportTo(jarFile);
            } finally {
                jarFile.flush();
                jarFile.close();
            }

        }
    }


    private static void copyFile(File target, InputStream src) throws IOException {
        Files.copy(src,  target.toPath());
    }

    private static File getModulePath() {
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

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "testglobal.jar");
        jar.addClass(GlobalModulesTestCase.class);
        return jar;

    }

    @ArquillianResource
    private InitialContext ctx;


    @Test
    public void testDataSourceDefinition() throws NamingException, SQLException {
        GlobalModuleEjb bean = (GlobalModuleEjb) ctx.lookup("java:module/" + GlobalModuleEjb.class.getSimpleName());
        Assert.assertEquals(GlobalModuleInterceptor.class.getSimpleName() + GlobalModuleEjb.class.getSimpleName(), bean.getName());
    }

}
