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

import org.jboss.as.ee.EeMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.ee.concurrent.ContextConfiguration;
import org.wildfly.ee.concurrent.ManagedThreadFactoryImpl;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryImplService implements Service<ManagedThreadFactoryImpl> {

    private final ContextConfiguration contextConfiguration;

    private String threadGroupName;
    private Integer priority;
    private String namePattern;
    private ManagedThreadFactoryImpl managedThreadFactory;

    public ManagedThreadFactoryImplService(ContextConfiguration contextConfiguration) {
        this.contextConfiguration = contextConfiguration;
    }

    public String getThreadGroupName() {
        return threadGroupName;
    }

    public void setThreadGroupName(final String threadGroupName) {
        this.threadGroupName = threadGroupName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(final String namePattern) {
        this.namePattern = namePattern;
    }

    public void start(final StartContext context) throws StartException {
        final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
        managedThreadFactory = new ManagedThreadFactoryImpl(contextConfiguration);
    }

    public void stop(final StopContext context) {
        managedThreadFactory.shutdownNow();
        managedThreadFactory = null;
    }

    public ManagedThreadFactoryImpl getValue() throws IllegalStateException {
        if (this.managedThreadFactory == null) {
            throw EeMessages.MESSAGES.concurrentServiceValueUninitialized();
        }
        return managedThreadFactory;
    }

}
