/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.ear;

import java.nio.charset.StandardCharsets;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EarLibClassPathTransitiveClosureTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class);
        libJar.addClasses(TestAA.class, EarLibClassPathTransitiveClosureTestCase.class);
        war.addAsLibraries(libJar);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class, "earLib.jar");
        earLib.addAsManifestResource(new ByteArrayAsset("Class-Path: ../cp1.jar\n".getBytes(StandardCharsets.UTF_8)), "MANIFEST.MF");
        ear.addAsLibraries(earLib);

        earLib = ShrinkWrap.create(JavaArchive.class, "cp1.jar");
        earLib.addAsManifestResource(new ByteArrayAsset("Class-Path: cp2.jar\n".getBytes(StandardCharsets.UTF_8)), "MANIFEST.MF");
        ear.addAsModule(earLib);

        earLib = ShrinkWrap.create(JavaArchive.class, "cp2.jar");
        earLib.addClass(TestBB.class);
        ear.addAsModule(earLib);
        return ear;
    }

    @Test
    public void testWebInfLibAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestAA");
    }

    @Test
    public void testClassPathEntryAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestBB");
    }

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
