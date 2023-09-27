/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.security.Principal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.concurrent.ManagedThreadFactory;

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
