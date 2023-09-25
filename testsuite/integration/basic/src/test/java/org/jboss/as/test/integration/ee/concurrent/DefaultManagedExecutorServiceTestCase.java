/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test for EE's default ManagedExecutorService
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultManagedExecutorServiceTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, DefaultManagedExecutorServiceTestCase.class.getSimpleName() + ".jar")
                .addClasses(DefaultManagedExecutorServiceTestCase.class, DefaultManagedExecutorServiceTestEJB.class, TestEJBRunnable.class, Util.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain"),
                                                                 new ElytronPermission("authenticate")), "permissions.xml");
    }

    @Test
    public void testTaskSubmit() throws Exception {
        final Callable<Void> callable = () -> {
            final DefaultManagedExecutorServiceTestEJB testEJB = (DefaultManagedExecutorServiceTestEJB) new InitialContext().lookup("java:module/" + DefaultManagedExecutorServiceTestEJB.class.getSimpleName());
            testEJB.submit(new TestEJBRunnable()).get();
            return null;
        };
        Util.switchIdentitySCF("guest", "guest", callable);

    }
}
