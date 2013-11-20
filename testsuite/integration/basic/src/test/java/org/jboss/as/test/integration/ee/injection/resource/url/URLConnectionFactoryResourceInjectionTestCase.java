/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
