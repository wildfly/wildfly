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
import java.util.concurrent.CountDownLatch;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import javax.resource.spi.IllegalStateException;

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
public class EEConcurrencyUtilitiesFunctionThreadFactoryTestEJB {

    private static final Logger logger = Logger.getLogger(EEConcurrencyUtilitiesFunctionThreadFactoryTestEJB.class);

    @Resource
    private EJBContext ejbContext;

    /**
     *
     * @param task
     * @throws Exception
     */
    public void run(final String jndiName, final TestEJBRunnable task) throws Exception {
        final ManagedThreadFactory managedThreadFactory = (ManagedThreadFactory) new InitialContext().lookup(jndiName);
        Assert.assertNotNull(managedThreadFactory);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final ExceptionWrapper ew = new ExceptionWrapper();
        Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                logger.debugf("Exception in thread: %s", ex.getMessage());
                ew.setT(ex);
                latch.countDown();
            }
        };
        
        final Principal principal = ejbContext.getCallerPrincipal();
        logger.debugf("Principal: %s", principal);
        task.setExpectedPrincipal(principal);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                task.run();
                latch.countDown();
            }
        };
        
        //Only for test -> it must throw Naming exception about ejbContext!!! + add extra dependency to Arqullian @Deployment...
        //ThreadFactory factory = new com.google.common.util.concurrent.ThreadFactoryBuilder().build();
        //Thread thread = factory.newThread(r);
        Thread thread = managedThreadFactory.newThread(r);
        thread.setUncaughtExceptionHandler(h);
        thread.start();
        latch.await();
        
        if(ew.getT() != null){
            throw new IllegalStateException(ew.getT());
        }    
    }
    
    private class ExceptionWrapper {
        Throwable t;

        public Throwable getT() {
            return t;
        }
        public void setT(Throwable t) {
            this.t = t;
        }
    }
}
