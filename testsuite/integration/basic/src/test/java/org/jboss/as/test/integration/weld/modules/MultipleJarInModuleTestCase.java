/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.weld.modules;

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

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Tests that multiple jars in a module are handled correctly
 *
 * see WFLY-3419
 *
 */
@RunWith(Arquillian.class)
public class MultipleJarInModuleTestCase {

    private static final List<TestModule> testModules = new ArrayList<>();

    public static void doSetup() throws Exception {
        URL url = MultipleJarInModuleTestCase.class.getResource("multiple-module.xml");
        File moduleXmlFile = new File(url.toURI());
        TestModule testModule = new TestModule("test.multiple", moduleXmlFile);
        JavaArchive jar = testModule.addResource("m1.jar");
        jar.addClass(Multiple1.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar = testModule.addResource("m2.jar");
        jar.addClass(Multiple2.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
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
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addClass(MultipleJarInModuleTestCase.class)
                .addClass(TestModule.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.multiple meta-inf\n"), "MANIFEST.MF");
        return war;
    }

    @Inject
    private Multiple2 multiple2;

    @Test
    public void testMultipleJarsInModule() {
        Assert.assertEquals("hello", multiple2.getMessage());

    }
}
