/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.optionalcomponent.cleanup;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that Weld cleanup is not causing deployment errors for composite deployment with optional web component.
 *
 * @see WFLY-10784
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
public class WeldCleanupWithOptionalWebComponentTest {

    @Deployment(name = "ear", testable = false)
    public static Archive<?> ear() {
        // create faulty WAR
        WebArchive war = ShrinkWrap.create(WebArchive.class, "WebComponentsIntegrationTestCase.war");
        war.addAsLibraries(
            ShrinkWrap.create(JavaArchive.class, "module.jar").addClass(StandardServletAsyncWebRequest.class));

        // create EJB JAR
        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "ejb.jar").addClass(EjbService.class);

        // add all to EAR
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "WebComponentsIntegrationTestCase.ear");
        ear.addAsModule(war);
        ear.addAsModule(ejb);
        return ear;
    }

    @Test
    @RunAsClient
    public void testEar() {
        // no-op the ability to deploy without error is the test here
    }

}
