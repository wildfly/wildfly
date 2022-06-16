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

package org.jboss.as.test.integration.ee.naming.defaultbindings.concurrency;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;

/**
 * @author Eduardo Martins
 */
@Stateless
public class DefaultConcurrencyTestEJB {

    @Resource
    private ContextService contextService;

    @Resource
    private ManagedExecutorService managedExecutorService;

    @Resource
    private ManagedScheduledExecutorService managedScheduledExecutorService;

    @Resource
    private ManagedThreadFactory managedThreadFactory;

    /**
     *
     * @throws Throwable
     */
    public void test() throws Throwable {
        // check injected resources
        if(contextService == null) {
            throw new NullPointerException("contextService");
        }
        if(managedExecutorService == null) {
            throw new NullPointerException("managedExecutorService");
        }
        if(managedScheduledExecutorService == null) {
            throw new NullPointerException("managedScheduledExecutorService");
        }
        if(managedThreadFactory == null) {
            throw new NullPointerException("managedThreadFactory");
        }
        // checked jndi lookup
        new InitialContext().lookup("java:comp/DefaultContextService");
        new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
        new InitialContext().lookup("java:comp/DefaultManagedScheduledExecutorService");
        new InitialContext().lookup("java:comp/DefaultManagedThreadFactory");
    }

}
