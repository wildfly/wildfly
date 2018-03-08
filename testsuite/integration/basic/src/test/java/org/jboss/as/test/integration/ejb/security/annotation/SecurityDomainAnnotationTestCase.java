/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ejb.security.annotation;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Tests if ERROR message is logged when org.jboss.security.annotation.SecurityDomain is used for specifying the JBoss security domain for EJBs
 * Test for [ WFLY-9168 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
public class SecurityDomainAnnotationTestCase {

    private static final String SECURITY_DOMAIN_DEPLOYMENT = "security-domain-deployment";

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = SECURITY_DOMAIN_DEPLOYMENT, managed = false, testable = false)
    public static Archive<?> deploy() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SECURITY_DOMAIN_DEPLOYMENT + ".jar");
        ejbJar.addClasses(SecurityDomainAnnotationTestCase.class, StatelessBean.class, StatelessInterface.class);
        ejbJar.addAsManifestResource(SecurityDomainAnnotationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return ejbJar;
    }

    @Test
    public void testSecurityDomainAnnotation() {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(baos));
            deployer.deploy(SECURITY_DOMAIN_DEPLOYMENT);
            try {
                System.setOut(oldOut);
                String output = new String(baos.toByteArray());
                Assert.assertTrue(output, output.contains("ERROR"));
                Assert.assertTrue(output, output.contains("Legacy org.jboss.security.annotation.SecurityDomain annotation is used in class"));
            } finally {
                deployer.undeploy(SECURITY_DOMAIN_DEPLOYMENT);
            }
        } finally {
            System.setOut(oldOut);
        }
    }
}
