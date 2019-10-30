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

import java.security.Principal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedThreadFactory;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * @author Eduardo Martins
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
@RolesAllowed("guest")
public class DefaultManagedThreadFactoryTestEJB {

    private static final Logger logger = Logger.getLogger(DefaultManagedThreadFactoryTestEJB.class);

    @Resource
    private ManagedThreadFactory managedThreadFactory;

    @Resource
    private EJBContext ejbContext;

    /**
     *
     * @param task
     * @throws Exception
     */
    public void run(final TestEJBRunnable task) throws Exception {
        final Principal principal = ejbContext.getCallerPrincipal();
        logger.debugf("Principal: %s", principal);
        task.setExpectedPrincipal(principal);
        final CountDownLatch latch = new CountDownLatch(1);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                task.run();
                latch.countDown();
            }
        };
        managedThreadFactory.newThread(r).start();
        if (! latch.await(TimeoutUtil.adjust(5000), TimeUnit.MILLISECONDS)) {
            Assert.fail("Thread not finished correctly");
        }
    }

}
