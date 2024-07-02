/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.executors;

import java.util.concurrent.ThreadFactory;

import org.infinispan.commons.executors.NonBlockingResource;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Thread factory for non-blocking threads.
 * @author Paul Ferraro
 */
public class DefaultNonBlockingThreadFactory extends DefaultThreadFactory implements NonBlockingResource {

    public DefaultNonBlockingThreadFactory(ThreadFactory factory) {
        super(factory, WildFlySecurityManager.getClassLoaderPrivileged(DefaultNonBlockingThreadFactory.class));
    }
}