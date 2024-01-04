/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.security.Principal;
import java.util.concurrent.Future;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author Eduardo Martins
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
@RolesAllowed("guest")
public class DefaultManagedExecutorServiceTestEJB {

    private static final Logger logger = Logger.getLogger(DefaultManagedExecutorServiceTestEJB.class);

    // keep the lookup, it's part of the test. It is not needed for injection of the default managed executor service, but the jndi name used when lookup is missing is the original one in java:jboss/ee/concurrent
    //@Resource(lookup = "java:comp/DefaultManagedExecutorService")
    @Resource
    private ManagedExecutorService executorService;

    @Resource
    private EJBContext ejbContext;

    /**
     * @param task
     * @return
     * @throws NamingException
     */
    public Future<?> submit(TestEJBRunnable task) throws NamingException {
        final Principal principal = ejbContext.getCallerPrincipal();
        logger.debugf("Principal: %s", principal);
        task.setExpectedPrincipal(principal);
        return executorService.submit(task);
    }

}
