/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.ear;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(ArquillianExtension.class)
public class EarJbossStructureDepedenciesTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addClasses(TestAA.class, EarJbossStructureDepedenciesTestCase.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        //test the 1.0 parser
        ear.addAsManifestResource(new StringAsset(
                        "<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.0\">" +
                        "<deployment>" +
                        "</deployment>" +
                        "<sub-deployment name=\"test.war\">" +
                        "   <dependencies>" +
                        "       <module name=\"org.jboss.classfilewriter\" />" +
                        "   </dependencies>" +
                        "</sub-deployment>" +
                "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class, "earLib.jar");
        earLib.addClass(TestBB.class);
        ear.addAsLibraries(earLib);
        ear.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return ear;
    }

    @Test
    public void testEarDoesNotHaveAccessToClassfilewriter() {
        assertThrows(ClassNotFoundException.class, () ->
            loadClass("org.jboss.classfilewriter.ClassFile", TestBB.class.getClassLoader()));
    }

    @Test
    public void testWarHasAccessToClassFileWriter() throws ClassNotFoundException {
        loadClass("org.jboss.classfilewriter.ClassFile", getClass().getClassLoader());
    }

    private static Class<?> loadClass(String name, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(name, false, cl);
    }
}
