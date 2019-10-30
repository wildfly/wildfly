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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.weld.modules.access;

import java.io.File;
import java.net.URL;

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

@RunWith(Arquillian.class)
public class BuiltInBeanWithPackagePrivateConstructorTest {

    @Inject
    private InjectedBean injectedBean;
    private static TestModule testModule;

    public static void doSetup() throws Exception {
        tearDown();

        URL url = BuiltInBeanWithPackagePrivateConstructor.class.getResource("test-module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule("test.module-accessibility", moduleXmlFile);
        JavaArchive jar = testModule.addResource("module-accessibility.jar");
        jar.addClass(BuiltInBeanWithPackagePrivateConstructor.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModule.create(true);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testModule != null) {
            testModule.remove();
        }
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();
        return ShrinkWrap.create(WebArchive.class).addClasses(InjectedBean.class, BuiltInBeanWithPackagePrivateConstructorTest.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.module-accessibility meta-inf\n"), "MANIFEST.MF");

    }

    @Test
    public void testBeanInjectable() throws IllegalArgumentException, IllegalAccessException {
        BuiltInBeanWithPackagePrivateConstructor bean = injectedBean.getBean();
        bean.setValue("foo");
        Assert.assertEquals("foo", bean.getValue());
    }
}
