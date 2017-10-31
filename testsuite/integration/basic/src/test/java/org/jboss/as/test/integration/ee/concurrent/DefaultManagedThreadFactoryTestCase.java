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

import java.util.PropertyPermission;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, DefaultManagedThreadFactoryTestCase.class.getSimpleName() + ".war")
                .addClasses(DefaultManagedThreadFactoryTestCase.class, DefaultManagedThreadFactoryTestEJB.class, TestEJBRunnable.class, Util.class, TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new ElytronPermission("getSecurityDomain"),
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
