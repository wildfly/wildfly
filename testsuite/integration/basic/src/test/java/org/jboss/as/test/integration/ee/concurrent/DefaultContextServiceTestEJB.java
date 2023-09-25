/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.security.Principal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.concurrent.ContextService;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author Eduardo Martins
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
@RolesAllowed("guest")
public class DefaultContextServiceTestEJB {

    private static final Logger logger = Logger.getLogger(DefaultContextServiceTestEJB.class);

    @Resource
    private ContextService contextService;

    @Resource
    private EJBContext ejbContext;

    /**
     * @param task
     * @return
     * @throws javax.naming.NamingException
     */
    public Future<?> submit(TestEJBRunnable task) throws NamingException {
        final Principal principal = ejbContext.getCallerPrincipal();
        logger.debugf("Principal: %s", principal);
        task.setExpectedPrincipal(principal);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            return executorService.submit(contextService.createContextualProxy(task,Runnable.class));
        } finally {
            executorService.shutdown();
        }
    }

}
