/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import org.wildfly.security.manager.WildFlySecurityManager;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;

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
        checkNotNullParamWithNullPointerException("contextService", contextService);
        checkNotNullParamWithNullPointerException("managedExecutorService", managedExecutorService);
        checkNotNullParamWithNullPointerException("managedScheduledExecutorService", managedScheduledExecutorService);
        checkNotNullParamWithNullPointerException("managedThreadFactory", managedThreadFactory);

        // WFLY-12039 regression check
        managedExecutorService.submit((Runnable) () -> {
            if (WildFlySecurityManager.getCurrentContextClassLoaderPrivileged() == null) {
                throw new IllegalStateException("WFLY-12039 regression, no TCCL found in task executed by non EE component");
            }
        }).get();
    }
}
