/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.dispatcher;

import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Non-clustered {@link CommandDispatcherFactory} implementation
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactory implements AutoCloseableCommandDispatcherFactory, PrivilegedAction<Void> {

    private static ThreadFactory createThreadFactory() {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(LocalCommandDispatcher.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return WildFlySecurityManager.doUnchecked(action);
    }

    private final Group group;
    private final ExecutorService executor;

    public LocalCommandDispatcherFactory(Group group) {
        this.group = group;
        this.executor = Executors.newCachedThreadPool(createThreadFactory());
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        return new LocalCommandDispatcher<>(this.group.getLocalMember(), context, this.executor);
    }

    @Override
    public void close() {
        WildFlySecurityManager.doUnchecked(this);
    }

    @Override
    public Void run() {
        this.executor.shutdownNow();
        return null;
    }
}
