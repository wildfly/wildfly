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
public class EarJbossStructureAdditionalModuleTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addClasses(TestAA.class, EarJbossStructureAdditionalModuleTestCase.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        ear.addAsManifestResource(new StringAsset(
                "<jboss-deployment-structure>" +
                        "<sub-deployment name=\"test.war\">" +
                        "<dependencies>" +
                        "<module name=\"deployment.somemodule\" />" +
                        "</dependencies>" +
                        "</sub-deployment>" +
                        "<module name=\"deployment.somemodule\">" +
                        "<resources><resource-root path=\"someModule.jar\"/></resources>" +
                        "</module>" +
                        "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class, "someModule.jar");
        earLib.addClass(TestBB.class);
        ear.addAsModule(earLib);
        return ear;
    }

    @Test
    public void testWarHassAccessToAdditionalModule() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestBB", getClass().getClassLoader());
    }

    private static Class<?> loadClass(String name, ClassLoader cl) throws ClassNotFoundException {
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
