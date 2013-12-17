/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import java.io.File;
import java.io.PrintWriter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This tests 'module add/remove' CLI command.
 *
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ModuleTestCase extends AbstractCliTestBase {

    private static final String MODULE_NAME = "org.jboss.test.cli.climoduletest";

    private static File jarFile;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "Dummy.jar");
        jar.addClass(ModuleTestCase.class);
        jarFile = new File(TestSuiteEnvironment.getTmpDir() + File.separator + "Dummy.jar");
        new ZipExporterImpl(jar).exportTo(jarFile, true);

        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        jarFile.delete();
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void addRemoveModule() throws Exception {
        testAddRemove("main");
    }

    @Test
    public void addRemoveModuleNonDefaultSlot() throws Exception {
        testAddRemove("2.0");
    }

    @Test
    public void addRemoveModuleMetaInf() throws Exception {
        testModuleFiles(false, "main");
        testAdd("main");
        testModuleFiles(true, "main");

        // create a META-INF directory inside the module
        final File metaInfDir = new File(getModulePath(), MODULE_NAME.replace('.', File.separatorChar) + File.separator + "main" + File.separator + "META-INF");
        if (!metaInfDir.mkdirs()) {
            fail("Could not create " + metaInfDir);
        }
        PrintWriter out = null;
        try {
            out = new PrintWriter(new File(metaInfDir, "version.txt"));
            out.println("main");
        } finally {
            IoUtils.safeClose(out);
        }

        testRemove("main");
        testModuleFiles(false, "main");
    }

    private void testAddRemove(String slotName) throws Exception {
        testModuleFiles(false, slotName);
        testAdd(slotName);
        testModuleFiles(true, slotName);
        testRemove(slotName);
        testModuleFiles(false, slotName);
    }

    private void testAdd(String slotName) throws Exception {
        // create a module
        cli.sendLine("module add --name=" + MODULE_NAME
                + ("main".equals(slotName) ? "" : " --slot=" + slotName)
                + " --resources=" + jarFile.getAbsolutePath());
    }

    private void testRemove(String slotName) throws Exception {
        // remove the module
        cli.sendLine("module remove --name=" + MODULE_NAME
                + ("main".equals(slotName) ? "" : " --slot=" + slotName));
    }

    private void testModuleFiles(boolean ifExists, String slotName) throws Exception {
        File testModuleRoot = new File(getModulePath(), MODULE_NAME.replace('.', File.separatorChar));
        assertTrue("Invalid state of module directory", ifExists == testModuleRoot.exists());

        File slot = new File(testModuleRoot, slotName);
        assertTrue("Invalid state of slot directory", ifExists == slot.exists());
    }

    private File getModulePath() {
   		String modulePath = TestSuiteEnvironment.getSystemProperty("module.path", null);
   		if (modulePath == null) {
   			String jbossHome = TestSuiteEnvironment.getSystemProperty("jboss.home", null);
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

}
