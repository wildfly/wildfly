/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.build.modular;

import java.io.File;

import org.jboss.as.build.modular.ModulesIntegrityChecker.ModulesIntegrityResult;
import org.jboss.modules.ModuleIdentifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test integrity of the modules hirarchy
 *
 * @author Thomas.Diesler@jboss.com
 * @since 15-Jul-2013
 */
public class ModulesIntegrityTestCase {

    File modulesDir;

    @Before
    public void setUp() {
        modulesDir = new File(System.getProperty("modules.path"));
        Assert.assertTrue("isDirectory: " + modulesDir, modulesDir.isDirectory());
    }

    @Test
    public void testModulesIntegrity() throws Exception {

        ModulesIntegrityChecker checker = new ModulesIntegrityChecker(modulesDir).addGlobalPackageIgnore("org.jboss.logging.annotations");
        checker.addModulePackageIgnore(ModuleIdentifier.fromString("org.jboss.common-core"), "org.apache.commons.httpclient", "org.apache.webdav.lib");

        ModulesIntegrityResult result = checker.checkIntegrity(ModuleIdentifier.fromString("org.jboss.vfs:main"));
        Assert.assertEquals(0, result.getErrorCount());
    }

    @Test
    public void testModulesIntegrityMain() throws Exception {
        String modulePath = modulesDir.getPath();
        String globalIgnore = "org.jboss.logging.annotations";
        String moduleIgnore = ModulesIntegrityTestCase.class.getClassLoader().getResource("test-module-package-ignore.txt").getPath();
        ModulesIntegrityChecker.main(new String[] { modulePath, globalIgnore, moduleIgnore, "true", "org.jboss.vfs:main" });
    }
}
