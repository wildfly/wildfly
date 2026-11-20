/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.export;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that an additional annotated type for a class that is not directly accessible
 * from the injection site but is accessible indirectly through exports is injectable.
 *
 * https://issues.jboss.org/browse/WFLY-4250
 *
 * @author Jozef Hartinger
 *
 */
@ExtendWith(ArquillianExtension.class)
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

    @AfterAll
    public static void tearDown() throws Exception {
        for (TestModule testModule : testModules) {
            testModule.remove();
        }
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
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
        Assertions.assertNotNull(b);
        Assertions.assertNotNull(c);
    }
}
