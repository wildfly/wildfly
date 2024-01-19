/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests accessibility between modules deployed in WF.
 *
 * There are four built-in modules installed at the beginning of the test case: alpha, bravo, charlie and delta.
 * The deployed testing application has a dependency on each of these modules. The following additional dependencies
 * exist:
 *
 * alpha -> bravo
 * bravo -> charlie
 *
 * Otherwise, the modules cannot access each other.
 *
 * @see WFLY-1746
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class CrossModuleAccessibilityTestCase {

    private static final List<TestModule> testModules = new ArrayList<>();

    public static void doSetup() throws Exception {
        addModule("alpha", "alpha-module.xml", AlphaBean.class, AlphaLookup.class);
        addModule("bravo", "bravo-module.xml", BravoBean.class, BravoLookup.class);
        addModule("charlie", "charlie-module.xml", CharlieBean.class, CharlieLookup.class);
        addModule("delta", "delta-module.xml", DeltaBean.class, DeltaLookup.class);
    }

    private static void addModule(String moduleName, String moduleXml, Class<?> beanType, Class<?> lookupType) throws Exception {
        URL url = beanType.getResource(moduleXml);
        File moduleXmlFile = new File(url.toURI());
        TestModule testModule = new TestModule("test." + moduleName, moduleXmlFile);
        JavaArchive jar = testModule.addResource(moduleName + ".jar");
        jar.addClass(beanType);
        jar.addClass(lookupType);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
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
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
        WebArchive war = ShrinkWrap.create(WebArchive.class, "CrossModuleAccessibilityTestCase.war")
                .addClass(CrossModuleAccessibilityTestCase.class)
                .addClass(TestModule.class)
                .addClass(GlowUtil.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.alpha meta-inf, test.bravo meta-inf, test.charlie meta-inf, test.delta meta-inf\n"), "MANIFEST.MF");
        return ShrinkWrap.create(EnterpriseArchive.class, "CrossModuleAccessibilityTestCase.ear").addAsModule(war);
    }

    @Test
    public void testAlphaModule(AlphaLookup alpha) {
        Assert.assertNotNull(alpha);
        Set<String> accessibleImplementations = getAccessibleImplementations(alpha.getInstance());
        Assert.assertTrue(accessibleImplementations.contains(AlphaBean.class.getSimpleName()));
        Assert.assertTrue(accessibleImplementations.contains(BravoBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(CharlieBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(DeltaBean.class.getSimpleName()));
    }

    @Test
    public void testBravoModule(BravoLookup bravo) {
        Assert.assertNotNull(bravo);
        Set<String> accessibleImplementations = getAccessibleImplementations(bravo.getInstance());
        Assert.assertFalse(accessibleImplementations.contains(AlphaBean.class.getSimpleName()));
        Assert.assertTrue(accessibleImplementations.contains(BravoBean.class.getSimpleName()));
        Assert.assertTrue(accessibleImplementations.contains(CharlieBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(DeltaBean.class.getSimpleName()));
    }

    @Test
    public void testCharlieModule(CharlieLookup charlie) {
        Assert.assertNotNull(charlie);
        Set<String> accessibleImplementations = getAccessibleImplementations(charlie.getInstance());
        Assert.assertFalse(accessibleImplementations.contains(AlphaBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(BravoBean.class.getSimpleName()));
        Assert.assertTrue(accessibleImplementations.contains(CharlieBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(DeltaBean.class.getSimpleName()));
    }

    @Test
    public void testDeltaModule(DeltaLookup delta) {
        Assert.assertNotNull(delta);
        Set<String> accessibleImplementations = getAccessibleImplementations(delta.getInstance());
        Assert.assertFalse(accessibleImplementations.contains(AlphaBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(BravoBean.class.getSimpleName()));
        Assert.assertFalse(accessibleImplementations.contains(CharlieBean.class.getSimpleName()));
        Assert.assertTrue(accessibleImplementations.contains(DeltaBean.class.getSimpleName()));
    }

    private Set<String> getAccessibleImplementations(Instance<Comparable<Integer>> instance) {
        Set<String> result = new HashSet<>();
        for (Object object : instance) {
            result.add(object.getClass().getSimpleName());
        }
        return result;
    }
}
