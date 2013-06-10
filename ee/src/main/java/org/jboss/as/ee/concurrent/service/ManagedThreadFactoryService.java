/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.concurrent.service;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.EeMessages;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryService implements Service<ManagedThreadFactoryImpl> {

    public static final Class<?> SERVICE_VALUE_TYPE = ManagedThreadFactoryImpl.class;

    private ManagedThreadFactoryImpl managedThreadFactory;

    private final String name;
    private final InjectedValue<ContextServiceImpl> contextService;
    private final int priority;

    /**
     * @param name
     * @param priority
     * @see ManagedThreadFactoryImpl#ManagedThreadFactoryImpl(String, org.glassfish.enterprise.concurrent.ContextServiceImpl, int)
     */
    public ManagedThreadFactoryService(String name, int priority) {
        this.name = name;
        this.contextService = new InjectedValue<>();
        this.priority = priority;
    }

    public void start(final StartContext context) throws StartException {
        managedThreadFactory = new ManagedThreadFactoryImpl(name, contextService.getOptionalValue(), priority);
    }

    public void stop(final StopContext context) {
        managedThreadFactory.stop();
        managedThreadFactory = null;
    }

    public ManagedThreadFactoryImpl getValue() throws IllegalStateException {
        if (this.managedThreadFactory == null) {
            throw EeMessages.MESSAGES.concurrentServiceValueUninitialized();
        }
        return managedThreadFactory;
    }

    public Injector<ContextServiceImpl> getContextServiceInjector() {
        return contextService;
    }

}
