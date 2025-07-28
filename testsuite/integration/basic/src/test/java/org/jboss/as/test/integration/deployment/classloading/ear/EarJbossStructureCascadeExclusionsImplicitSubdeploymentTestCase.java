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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EarJbossStructureCascadeExclusionsImplicitSubdeploymentTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive implicitWar = ShrinkWrap.create(WebArchive.class, "implicit.war");
        implicitWar.addClasses(TestBB.class, EarJbossStructureCascadeExclusionsImplicitSubdeploymentTestCase.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(implicitWar);

        ear.addAsManifestResource(new StringAsset(
                        "<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.3\">" +
                        "<ear-exclusions-cascaded-to-subdeployments>true</ear-exclusions-cascaded-to-subdeployments>" +
                        "<deployment>" +
                        "   <exclusions>" +
                        "      <module name=\"org.jboss.logging\" />" +
                        "   </exclusions>" +
                        "</deployment>" +
                "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        return ear;
    }

    @Test(expected = ClassNotFoundException.class)
    public void testImplicitWarDoesNotHaveAccessToJbossLogging() throws ClassNotFoundException {
        loadClass("org.jboss.logging.Logger", getClass().getClassLoader());
    }

    @Test
    public void testImplicitWarDoesHaveAccessToSlf4jLogger() throws ClassNotFoundException {
        loadClass("org.slf4j.Logger", getClass().getClassLoader());
    }

    private static Class<?> loadClass(String name, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(name, false, cl);
    }
}
