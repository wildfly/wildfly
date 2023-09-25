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
public class WarCanAccessEjbJarTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "otherjar.jar");
        jar.addClass(TestAA.class);
        jar.addAsResource(emptyEjbJar(), "META-INF/ejb-jar.xml");

        ear.addAsModule(jar);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testwar.war");
        war.addClass(WarCanAccessEjbJarTestCase.class);
        war.addAsResource(new StringAsset("Class-Path: otherjar.jar\n"),"META-INF/MANIFEST.MF");
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testEjbJarCanAccessOtherEjbJar() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestAA");
    }


    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }

    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ejb-jar xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" \n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "         xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"\n" +
                "         version=\"4.0\">\n" +
                "   \n" +
                "</ejb-jar>");
    }
}
