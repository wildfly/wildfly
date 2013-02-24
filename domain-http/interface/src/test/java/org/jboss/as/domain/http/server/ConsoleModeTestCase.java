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
package org.jboss.as.domain.http.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.junit.Test;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConsoleModeTestCase {


    @Test
    public void testDefaultModules() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console"), "modules-default");
    }


    @Test
    public void testVersionedNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "1.2.1"), "modules-versioned");
    }

    @Test
    public void testVersionedAndMainSlot() throws Exception {
        checkModule("main", ModuleIdentifier.create("org.jboss.as.console.main", "1.2.1"), "modules-versioned");
    }

    @Test
    public void testVersionedLayersNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "1.2.1"), "modules-base-and-layer1");
    }

    @Test
    public void testVersionedLayersAndMainSlot() throws Exception {
        checkModule("main", ModuleIdentifier.create("org.jboss.as.console.main", "1.2.1"), "modules-base-and-layer1");
    }

    @Test
    public void testSeveralRootsVersionedLayersNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "3.0.0"), "modules-base-and-layer1", "modules-layer2");
    }

    @Test
    public void testSeveralRootsVersionedLayersAndMainSlot() throws Exception {
        checkModule("main", ModuleIdentifier.create("org.jboss.as.console.main", "3.0.0"), "modules-base-and-layer1", "modules-layer2");
    }

    @Test
    public void testSeveralRootsDifferentOrderVersionedLayersNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "3.0.0"), "modules-layer2", "modules-base-and-layer1");
    }

    @Test
    public void testSeveralRootsDifferentOrderVersionedLayersAndMainSlot() throws Exception {
        checkModule("main", ModuleIdentifier.create("org.jboss.as.console.main", "3.0.0"), "modules-layer2", "modules-base-and-layer1");
    }

    @Test
    public void testAddonsAndLayersAddon1WinsNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "2.0.0"), "modules-base-and-layer1", "modules-addons1");
    }

    @Test
    public void testAddonsAndLayersLayer2WinsNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "3.0.0"), "modules-base-and-layer1", "modules-layer2", "modules-addons1");
    }

    @Test
    public void testAddonsAndLayersAddon2WinsNoSlot() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "4.0.0"), "modules-base-and-layer1", "modules-layer2", "modules-addons1", "modules-addons2");
    }

    @Test
    public void testAddonsOnly() throws Exception {
        checkModule(null, ModuleIdentifier.create("org.jboss.as.console.main", "4.0.0"), "modules-addons1", "modules-addons2");
    }

    private void checkModule(String slot, ModuleIdentifier expected, String...moduleDirNames) throws Exception {
        ModuleLoader loader = createModuleLoader(moduleDirNames);
        ClassLoader classLoader = ConsoleMode.ConsoleHandler.findConsoleClassLoader(loader, slot);
        assertNotNull(classLoader);
        assertTrue(classLoader instanceof ModuleClassLoader);
        ModuleClassLoader moduleClassLoader = (ModuleClassLoader)classLoader;
        assertEquals(expected, moduleClassLoader.getModule().getIdentifier());
    }

    private ModuleLoader createModuleLoader(String...moduleDirNames) {
        StringBuilder sb = new StringBuilder();
        for (String moduleDirName : moduleDirNames) {
            File file = new File("target/test-classes", moduleDirName);
            assertTrue(file.exists());
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(file.getAbsolutePath());
        }
        System.setProperty("module.path", sb.toString());
        LocalModuleLoader loader = new LocalModuleLoader();
        return loader;
    }
}
