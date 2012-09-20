/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.osgi.classloading;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.osgi.classloading.bundle.TestCC;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-945] Cannot deploy EAR/WAR with nested bundle
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Aug-2011
 */
@RunWith(Arquillian.class)
public class BundleNestedInEarTestCase {

    private static final String TEST_CLASS = BundleNestedInEarTestCase.class.getName();
    private static final String TEST_CC = "org.jboss.as.test.integration.osgi.classloading.bundle.TestCC";

    private static final String SIMPLE_EAR = "simple.ear";
    private static final String NESTED_BUNDLE_AS_LIB_EAR = "nested-bundle-as-lib.ear";
    private static final String NESTED_BUNDLE_AS_MODULE_EAR = "nested-bundle-as-module.ear";

    @Deployment(name = SIMPLE_EAR)
    public static Archive<?> getSimpleEar() {
        JavaArchive jarA = ShrinkWrap.create(JavaArchive.class, "jarA.jar");
        jarA.addClass(BundleNestedInEarTestCase.class);

        JavaArchive jarB = ShrinkWrap.create(JavaArchive.class, "jarB.jar");
        jarB.addClass(TestCC.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsLibrary(jarA);
        ear.addAsLibrary(jarB);
        return ear;
    }

    @Deployment(name = NESTED_BUNDLE_AS_LIB_EAR)
    public static Archive<?> getNestedBundleAsLibEar() {
        JavaArchive jarA = ShrinkWrap.create(JavaArchive.class, "jarA.jar");
        jarA.addClass(BundleNestedInEarTestCase.class);

        final JavaArchive bundle = ShrinkWrap.create(JavaArchive.class, "nested-bundle.jar");
        bundle.addClass(TestCC.class);
        bundle.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(bundle.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, NESTED_BUNDLE_AS_LIB_EAR);
        ear.addAsLibrary(jarA);
        ear.addAsLibrary(bundle);
        return ear;
    }

    @Deployment(name = NESTED_BUNDLE_AS_MODULE_EAR)
    public static Archive<?> getNestedBundleAsModuleEar() {
        JavaArchive jarA = ShrinkWrap.create(JavaArchive.class, "jarA.jar");
        jarA.addClass(BundleNestedInEarTestCase.class);

        final JavaArchive bundle = ShrinkWrap.create(JavaArchive.class, "nested-bundle.jar");
        bundle.addClass(TestCC.class);
        bundle.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(bundle.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, NESTED_BUNDLE_AS_MODULE_EAR);
        ear.addAsLibrary(jarA);
        ear.addAsModule(bundle);
        return ear;
    }

    @Test
    @OperateOnDeployment(SIMPLE_EAR)
    public void testNestedJars() throws Exception {
        ModuleIdentifier earModuleId = ModuleIdentifier.fromString("deployment.simple.ear:main");
        Module earModule = getModuleLoader().loadModule(earModuleId);
        Class<?> testClass = earModule.getClassLoader().loadClass(TEST_CLASS);
        ModuleClassLoader testClassLoader = (ModuleClassLoader) testClass.getClassLoader();
        Assert.assertEquals(earModuleId, testClassLoader.getModule().getIdentifier());
        Class<?> classCC = testClassLoader.loadClass(TEST_CC);
        ModuleClassLoader classLoaderCC = (ModuleClassLoader) classCC.getClassLoader();
        Assert.assertEquals(earModuleId, classLoaderCC.getModule().getIdentifier());
    }

    @Test
    @OperateOnDeployment(NESTED_BUNDLE_AS_LIB_EAR)
    public void testNestedBundleAsLib() throws Exception {
        ModuleIdentifier earModuleId = ModuleIdentifier.fromString("deployment.nested-bundle-as-lib.ear:main");
        Module earModule = getModuleLoader().loadModule(earModuleId);
        Class<?> testClass = earModule.getClassLoader().loadClass(TEST_CLASS);
        ModuleClassLoader testClassLoader = (ModuleClassLoader) testClass.getClassLoader();
        Assert.assertEquals(earModuleId, testClassLoader.getModule().getIdentifier());
        Class<?> classCC = testClassLoader.loadClass(TEST_CC);
        ModuleClassLoader classLoaderCC = (ModuleClassLoader) classCC.getClassLoader();
        Assert.assertEquals(earModuleId, classLoaderCC.getModule().getIdentifier());
    }

    @Test
    @OperateOnDeployment(NESTED_BUNDLE_AS_MODULE_EAR)
    public void testNestedBundleAsModule() throws Exception {
        ModuleIdentifier earModuleId = ModuleIdentifier.fromString("deployment.nested-bundle-as-module.ear:main");
        Module earModule = getModuleLoader().loadModule(earModuleId);
        Class<?> testClass = earModule.getClassLoader().loadClass(TEST_CLASS);
        ModuleClassLoader testClassLoader = (ModuleClassLoader) testClass.getClassLoader();
        Assert.assertEquals(earModuleId, testClassLoader.getModule().getIdentifier());
        try {
            testClassLoader.loadClass(TEST_CC);
            Assert.fail("ClassNotFoundException expected");
        } catch (ClassNotFoundException ex) {
            // expected
        }
        ModuleIdentifier bundleModuleId = ModuleIdentifier.fromString("deployment.nested-bundle-as-module.ear.nested-bundle.jar:main");
        Module bundleModule = getModuleLoader().loadModule(bundleModuleId);
        Class<?> classCC = bundleModule.getClassLoader().loadClass(TEST_CC);
        ModuleClassLoader classLoaderCC = (ModuleClassLoader) classCC.getClassLoader();
        Assert.assertEquals(bundleModuleId, classLoaderCC.getModule().getIdentifier());
    }

    private ModuleLoader getModuleLoader() {
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
        return classLoader.getModule().getModuleLoader();
    }
}
