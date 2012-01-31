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

package org.jboss.as.threads;

import java.security.AccessController;
import java.util.concurrent.ThreadFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadFactoryService implements Service<ThreadFactory> {
    private String threadGroupName;
    private Integer priority;
    private String namePattern;
    private ThreadFactory value;

    public synchronized String getThreadGroupName() {
        return threadGroupName;
    }

    public synchronized void setThreadGroupName(final String threadGroupName) {
        this.threadGroupName = threadGroupName;
    }

    public synchronized Integer getPriority() {
        return priority;
    }

    public synchronized void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public synchronized String getNamePattern() {
        return namePattern;
    }

    public synchronized void setNamePattern(final String namePattern) {
        this.namePattern = namePattern;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
        value = new JBossThreadFactory(threadGroup, Boolean.FALSE, priority, namePattern, null, null, AccessController.getContext());
    }

    public synchronized void stop(final StopContext context) {
        value = null;
    }

    public synchronized ThreadFactory getValue() throws IllegalStateException {
        final ThreadFactory value = this.value;
        if (value == null) {
            throw ThreadsMessages.MESSAGES.threadFactoryUninitialized();
        }
        return value;
    }
}
