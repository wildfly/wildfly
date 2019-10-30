/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security;

import java.util.concurrent.Callable;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.dd.override.PartialDDBean;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests that security configurations on an EJB, overriden through the use of ejb-jar.xml work as expected
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup({EjbSecurityDomainSetup.class})
@Category(CommonCriteria.class)
public class SecurityDDOverrideTestCase {

    private static final Logger logger = Logger.getLogger(SecurityDDOverrideTestCase.class);

    @Deployment
    public static Archive<?> runAsDeployment() {
        final Package currentPackage = SecurityDDOverrideTestCase.class.getPackage();
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-security-partial-dd-test.jar")
                .addPackage(PartialDDBean.class.getPackage())
                .addClass(Util.class)
                .addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class)
                .addAsResource(currentPackage, "users.properties", "users.properties")
                .addAsResource(currentPackage, "roles.properties", "roles.properties")
                .addAsManifestResource(currentPackage, "partial-ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF")
                .addAsManifestResource(currentPackage, "permissions.xml", "permissions.xml")
                .addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    /**
     * Tests that the overriden roles allowed, via ejb-jar.xml, on an EJB method are taken into account for EJB method
     * invocations
     *
     * @throws Exception
     */
    @Test
    public void testDDOverride() throws Exception {
        final Context ctx = new InitialContext();
        final PartialDDBean partialDDBean = (PartialDDBean) ctx.lookup("java:module/" + PartialDDBean.class.getSimpleName() + "!" + PartialDDBean.class.getName());
        try {
            partialDDBean.denyAllMethod();
            Assert.fail("Call to denyAllMethod() was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }
        // expected to pass
        partialDDBean.permitAllMethod();

        // login as user1 and test
        Callable<Void> callable = () -> {
            // expected to pass since user1 belongs to Role1
            partialDDBean.toBeInvokedOnlyByRole1();

            // expected to fail since user1 *doesn't* belong to Role2
            try {
                partialDDBean.toBeInvokedByRole2();
                Assert.fail("Call to toBeInvokedByRole2() was expected to fail");
            } catch (EJBAccessException ejbae) {
                // expected
            }
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);

        // login as user2 and test
        callable = () -> {
            // expected to pass since user2 belongs to Role2
            partialDDBean.toBeInvokedByRole2();

            // expected to fail since user2 *doesn't* belong to Role1
            try {
                partialDDBean.toBeInvokedOnlyByRole1();
                Assert.fail("Call to toBeInvokedOnlyByRole1() was expected to fail");
            } catch (EJBAccessException ejbae) {
                // expected
            }
            return null;
        };
        Util.switchIdentity("user2", "password2", callable);


    }
}
