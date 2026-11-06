/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.url;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;

/**
 * Test for EE's resource injection for URLs
 *
 * @author Eduardo Martins
 */
@ExtendWith(ArquillianExtension.class)
public class URLConnectionFactoryResourceInjectionTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, URLConnectionFactoryResourceInjectionTestCase.class.getSimpleName() + ".jar")
                .addClasses(URLConnectionFactoryResourceInjectionTestCase.class, URLConnectionFactoryResourceInjectionTestEJB.class)
                .addAsManifestResource(URLConnectionFactoryResourceInjectionTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
    }

    @Test
    public void testResourceInjection() throws Exception {
        final URLConnectionFactoryResourceInjectionTestEJB testEJB = (URLConnectionFactoryResourceInjectionTestEJB) new InitialContext().lookup("java:module/" + URLConnectionFactoryResourceInjectionTestEJB.class.getSimpleName());
        testEJB.validateResourceInjection();
    }
}
