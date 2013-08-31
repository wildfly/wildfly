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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.util.concurrent.TimeUnit;

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
                .addClasses(DefaultManagedScheduledExecutorServiceTestCase.class, DefaultManagedScheduledExecutorServiceTestEJB.class, TestEJBRunnable.class);
    }

    @Test
    public void testTaskSubmit() throws Exception {
        SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("guest", "guest");
        client.login();
        try {
            final DefaultManagedScheduledExecutorServiceTestEJB testEJB = (DefaultManagedScheduledExecutorServiceTestEJB) new InitialContext().lookup("java:module/" + DefaultManagedScheduledExecutorServiceTestEJB.class.getSimpleName());
            testEJB.schedule(new TestEJBRunnable(), 10L, TimeUnit.MILLISECONDS).get();
        } finally {
            client.logout();
        }

    }
}
