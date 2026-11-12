/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.ear;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class EarJbossStructureCascadeExclusionsExplicitSubdeploymentTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive explicitWar = ShrinkWrap.create(WebArchive.class, "explicit.war");
        explicitWar.addClasses(TestAA.class, EarJbossStructureCascadeExclusionsExplicitSubdeploymentTestCase.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(explicitWar);
        //test the 1.3 structure parser cascade exclusions
        ear.addAsManifestResource(new StringAsset(
                        "<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.3\">" +
                        "<ear-exclusions-cascaded-to-subdeployments>true</ear-exclusions-cascaded-to-subdeployments>" +
                        "<deployment>" +
                        "   <exclusions>" +
                        "      <module name=\"org.jboss.logging\" />" +
                        "   </exclusions>" +
                        "</deployment>" +
                        "<sub-deployment name=\"explicit.war\">" +
                        "   <dependencies>" +
                        "       <module name=\"org.jboss.classfilewriter\" />" +
                        "   </dependencies>" +
                        "</sub-deployment>" +
                "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        return ear;
    }

    @Test
    public void testWarDoesNotHaveAccessToClassJbossLoggingLogger() {
        assertThrows(ClassNotFoundException.class, () ->
            loadClass("org.jboss.logging.Logger", getClass().getClassLoader()));
    }

    @Test
    public void testWarDoesHaveAccessToClassSlf4jLogger() throws ClassNotFoundException {
        loadClass("org.slf4j.Logger", getClass().getClassLoader());
    }

    private static Class<?> loadClass(String name, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(name, false, cl);
    }
}
