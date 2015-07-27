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

package org.jboss.as.test.integration.ee.concurrent.function;

import java.security.Principal;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.test.integration.ee.concurrent.TestEJBRunnable;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * @author Eduardo Martins
 * @author Hynek Svabek
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
@RolesAllowed("guest")
public class EEConcurrencyUtilitiesManagedExecutorServiceTestEJB {

    private static final Logger logger = Logger.getLogger(EEConcurrencyUtilitiesManagedExecutorServiceTestEJB.class);


    @Resource
    private EJBContext ejbContext;

    /**
     * @param task
     * @return
     * @throws NamingException
     */
    public Future<?> submit(String jndiName, TestEJBRunnable task) throws NamingException {
        final ManagedExecutorService executorService = (ManagedExecutorService) new InitialContext().lookup(jndiName);
        Assert.assertNotNull(executorService);
        
        final Principal principal = ejbContext.getCallerPrincipal();
        logger.debugf("Principal: %s", principal);
        task.setExpectedPrincipal(principal);
        return executorService.submit(task);
    }

}
