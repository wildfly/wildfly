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
package org.jboss.as.test.integration.deployment.classloading.ear;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * tests that dependencies added to items in the ear are propagated to sub deployments.
 */
@RunWith(Arquillian.class)
public class EarManifestDependencyPropagatedTestCase {

    public static final String CLASS_FILE_WRITER_CLASS = "org.jboss.classfilewriter.ClassFile";
    public static final String JANDEX_CLASS = "org.jboss.jandex.Index";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,"eartest.ear");
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class,"ejbmodule.jar");
        ejbJar.addClasses(EarManifestDependencyPropagatedTestCase.class);
        ejbJar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.jandex\n"),"MANIFEST.MF");
        ear.addAsModule(ejbJar);

        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class, "libjar.jar");
        earLib.addAsManifestResource(new StringAsset("Dependencies: org.jboss.classfilewriter\n"),"MANIFEST.MF");
        earLib.addClass(EarLibClassLoadingClass.class);

        ear.addAsLibraries(earLib);
        return ear;
    }

    @Test
    public void testClassFileWriterAccessible() throws ClassNotFoundException {
        loadClass(CLASS_FILE_WRITER_CLASS);
        EarLibClassLoadingClass.loadClass(CLASS_FILE_WRITER_CLASS);
    }

    @Test
    public void testJandexAccessibility() throws ClassNotFoundException {
        loadClass(JANDEX_CLASS);
        try {
            EarLibClassLoadingClass.loadClass(JANDEX_CLASS);
            Assert.fail("Expected class loading to fail");
        } catch (ClassNotFoundException e) {

        }
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
                "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                "         version=\"3.0\">\n" +
                "   \n" +
                "</ejb-jar>");
    }
}
