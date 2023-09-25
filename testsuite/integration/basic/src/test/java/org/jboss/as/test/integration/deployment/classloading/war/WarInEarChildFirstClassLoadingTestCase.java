/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.war;

import jakarta.ejb.Stateless;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.deployment.classloading.ear.TestBB;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.test.integration.common.WebInfLibClass;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

@RunWith(Arquillian.class)
public class WarInEarChildFirstClassLoadingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClasses(WebInfLibClass.class, WarInEarChildFirstClassLoadingTestCase.class, Stateless.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class, "cp.jar");
        earLib.addAsManifestResource(new StringAsset("Dependencies: \n"), "MANIFEST.MF"); //AS7-5547, make sure an empty dependencies entry is fine
        earLib.addClasses(TestBB.class, WebInfLibClass.class);
        ear.addAsLibrary(earLib);
        ear.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return ear;
    }

    @Test
    public void testChildFirst() throws ClassNotFoundException {
        Assert.assertNotSame(Stateless.class.getClassLoader(), getClass().getClassLoader());
    }

    @Test
    public void testMultipleClasses() throws ClassNotFoundException {
        Class<?> clazz = loadClass(WebInfLibClass.class.getName(), TestBB.class.getClassLoader());
        Assert.assertNotSame(WebInfLibClass.class, clazz);
    }

    private static Class<?> loadClass(String name, ClassLoader cl) throws ClassNotFoundException {
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
