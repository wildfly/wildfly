/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.concurrency;

import org.wildfly.security.manager.WildFlySecurityManager;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;

/**
 * @author Eduardo Martins
 */
public class DefaultConcurrencyTestCDIBean {

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
        // WFLY-12039 regression check
        managedExecutorService.submit((Runnable) () -> {
            if (WildFlySecurityManager.getCurrentContextClassLoaderPrivileged() == null) {
                throw new IllegalStateException("WFLY-12039 regression, no TCCL found in task executed by non EE component");
            }
        }).get();
    }
}
