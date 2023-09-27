/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

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

/**
 * Test for EE's default context service
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultContextServiceTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, DefaultContextServiceTestCase.class.getSimpleName() + ".jar")
                .addClasses(DefaultContextServiceTestCase.class, DefaultContextServiceTestEJB.class, TestEJBRunnable.class, Util.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new RuntimePermission("modifyThread"),
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                        ), "permissions.xml");
    }

    @Test
    public void testTaskSubmit() throws Exception {
        final Callable<Void> callable = () -> {
            final DefaultContextServiceTestEJB testEJB = (DefaultContextServiceTestEJB) new InitialContext().lookup("java:module/" + DefaultContextServiceTestEJB.class.getSimpleName());
            testEJB.submit(new TestEJBRunnable()).get();
            return null;
        };
        Util.switchIdentitySCF("guest", "guest", callable);

    }
}
