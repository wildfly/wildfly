/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.security.Principal;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author Eduardo Martins
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
@RolesAllowed("guest")
public class DefaultManagedScheduledExecutorServiceTestEJB {

    private static final Logger logger = Logger.getLogger(DefaultManagedScheduledExecutorServiceTestEJB.class);

    // keep the name, it's part of the test. If only the name is set then injection source should be the default managed scheduled executor service
    @Resource(name = "blabla")
    private ManagedScheduledExecutorService executorService;

    @Resource
    private EJBContext ejbContext;

    /**
     * @param task
     * @param delay
     * @param timeUnit
     * @return
     * @throws NamingException
     */
    public Future<?> schedule(TestEJBRunnable task, long delay, TimeUnit timeUnit) throws NamingException {
        final Principal principal = ejbContext.getCallerPrincipal();
        logger.debugf("Principal: %s", principal);
        task.setExpectedPrincipal(principal);
        return executorService.schedule(task, delay, timeUnit);
    }

}
