/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.url;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Test for EE's resource injection for URLs
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
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
