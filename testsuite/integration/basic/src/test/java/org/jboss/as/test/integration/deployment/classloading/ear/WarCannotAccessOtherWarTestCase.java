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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class WarCannotAccessOtherWarTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(TestAA.class);

        ear.addAsModule(war);
        war = ShrinkWrap.create(WebArchive.class);
        war.addClass(WarCannotAccessOtherWarTestCase.class);
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testWarCannotAccessOtherWar() {
        assertThrows(ClassNotFoundException.class, () ->
            loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestAA"));
    }


    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
