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

package org.jboss.as.test.integration.ee.concurrent;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, DefaultManagedScheduledExecutorServiceTestCase.class.getSimpleName() + ".war")
                .addClasses(DefaultManagedScheduledExecutorServiceTestCase.class, DefaultManagedScheduledExecutorServiceTestEJB.class, TestEJBRunnable.class, Util.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
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
