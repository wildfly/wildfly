/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Test for EE's default managed thread factory
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultManagedThreadFactoryTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, DefaultManagedThreadFactoryTestCase.class.getSimpleName() + ".jar")
                .addClasses(DefaultManagedThreadFactoryTestCase.class, DefaultManagedThreadFactoryTestEJB.class, TestEJBRunnable.class, Util.class, TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate"),
                        new PropertyPermission("ts.timeout.factor", "read")
                        ), "permissions.xml");
    }

    @Test
    public void testTaskSubmit() throws Exception {
        final Callable<Void> callable = () -> {
            final DefaultManagedThreadFactoryTestEJB testEJB = (DefaultManagedThreadFactoryTestEJB) new InitialContext().lookup("java:module/" + DefaultManagedThreadFactoryTestEJB.class.getSimpleName());
            testEJB.run(new TestEJBRunnable());
            return null;
        };
        Util.switchIdentitySCF("guest", "guest", callable);
    }
}
