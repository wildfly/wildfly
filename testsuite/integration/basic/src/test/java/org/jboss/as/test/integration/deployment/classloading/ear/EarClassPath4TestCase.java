/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.ear;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EarClassPath4TestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "cptest.ear");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war");
        war.addAsResource(new StringAsset("Class-Path: ejb-jar.jar other-jar.jar \n"),"META-INF/MANIFEST.MF");
        war.addClasses(EarClassPath4TestCase.class);

        ear.addAsModule(war);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-jar.jar");
        jar.addClass(MyEjb.class);
        ear.addAsModule(jar);

        jar = ShrinkWrap.create(JavaArchive.class, "other-jar.jar");
        jar.addClass(TestAA.class);
        ear.addAsModule(jar);

        return ear;
    }

    @Test
    public void testClassPathEjbJarAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.MyEjb");
    }

    @Test
    public void testClassPathOtherJarAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestAA");
    }

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
