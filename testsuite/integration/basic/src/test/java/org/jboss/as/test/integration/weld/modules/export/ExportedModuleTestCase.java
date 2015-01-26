/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.modules.export;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that an additional annotated type for a class that is not directly accessible
 * from the injection site but is accessible indirectly through exports is injectable.
 *
 * https://issues.jboss.org/browse/WFLY-4250
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class ExportedModuleTestCase {

    private static final List<TestModule> testModules = new ArrayList<>();

    public static void doSetup() throws Exception {
        addModule("export-alpha", "alpha-module.xml", true, AlphaBean.class, AlphaExtension.class);
        addModule("export-bravo", "bravo-module.xml", false, BravoBean.class, null);
        addModule("export-charlie", "charlie-module.xml", false, CharlieBean.class, null);
    }

    private static void addModule(String moduleName, String moduleXml, boolean beanArchive, Class<?> beanType, Class<? extends Extension> extension) throws Exception {
        URL url = beanType.getResource(moduleXml);
        File moduleXmlFile = new File(url.toURI());
        TestModule testModule = new TestModule("test." + moduleName, moduleXmlFile);
        JavaArchive jar = testModule.addResource(moduleName + ".jar");
        jar.addClass(beanType);
        if (beanArchive) {
            jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        }
        if (extension != null) {
            jar.addClass(extension);
            jar.addAsServiceProvider(Extension.class, extension);
        }
        testModules.add(testModule);
        testModule.create(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        for (TestModule testModule : testModules) {
            testModule.remove();
        }
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();
        return ShrinkWrap.create(WebArchive.class)
                .addClass(ExportedModuleTestCase.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.export-alpha meta-inf\n"), "MANIFEST.MF");
    }

    @Inject
    BravoBean b;
    @Inject
    CharlieBean c;

    @Test
    public void testClassFromExportedDependencyAccessible() {
        Assert.assertNotNull(b);
        Assert.assertNotNull(c);
    }
}
