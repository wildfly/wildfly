/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
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

/**
 * Test for EE's default ManagedScheduledExecutorService
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultManagedScheduledExecutorServiceTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, DefaultManagedScheduledExecutorServiceTestCase.class.getSimpleName() + ".jar")
                .addClasses(DefaultManagedScheduledExecutorServiceTestCase.class, DefaultManagedScheduledExecutorServiceTestEJB.class, TestEJBRunnable.class, Util.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain"),
                                                                 new ElytronPermission("authenticate")), "permissions.xml");
    }

    @Test
    public void testTaskSubmit() throws Exception {
        final Callable<Void> callable = () -> {
            final DefaultManagedScheduledExecutorServiceTestEJB testEJB = (DefaultManagedScheduledExecutorServiceTestEJB) new InitialContext().lookup("java:module/" + DefaultManagedScheduledExecutorServiceTestEJB.class.getSimpleName());
            testEJB.schedule(new TestEJBRunnable(), 10L, TimeUnit.MILLISECONDS).get();
            return null;
        };
        Util.switchIdentitySCF("guest", "guest", callable);

    }
}
