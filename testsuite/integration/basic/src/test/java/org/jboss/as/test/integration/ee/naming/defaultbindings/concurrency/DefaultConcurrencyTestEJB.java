/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
