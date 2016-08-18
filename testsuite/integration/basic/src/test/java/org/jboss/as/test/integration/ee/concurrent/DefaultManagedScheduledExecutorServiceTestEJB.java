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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
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
