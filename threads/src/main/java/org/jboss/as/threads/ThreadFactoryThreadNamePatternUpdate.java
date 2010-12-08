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

import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadFactoryThreadNamePatternUpdate extends AbstractThreadsSubsystemUpdate<Void> {

    private static final long serialVersionUID = 4253625376544201028L;

    private final String name;
    private final String newNamePattern;

    public ThreadFactoryThreadNamePatternUpdate(final String name, final String newNamePattern) {
        this.name = name;
        this.newNamePattern = newNamePattern;
    }

    public ThreadFactoryThreadNamePatternUpdate getCompensatingUpdate(final ThreadsSubsystemElement original) {
        final ThreadFactoryElement threadFactory = original.getThreadFactory(name);
        if (threadFactory == null) {
            return null;
        }
        return new ThreadFactoryThreadNamePatternUpdate(name, threadFactory.getGroupName());
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        final ServiceController<?> service = updateContext.getServiceRegistry().getService(ThreadsServices.threadFactoryName(name));
        if (service == null) {
            handler.handleFailure(notConfigured(), param);
        } else {
            try {
                final ThreadFactoryService threadFactoryService = (ThreadFactoryService) service.getValue();
                threadFactoryService.setNamePattern(newNamePattern);
                handler.handleSuccess(null, param);
            } catch (Throwable t) {
                handler.handleFailure(t, param);
            }
        }
    }

    protected void applyUpdate(final ThreadsSubsystemElement element) throws UpdateFailedException {
        final ThreadFactoryElement threadFactory = element.getThreadFactory(name);
        if (threadFactory == null) {
            throw notConfigured();
        }
        threadFactory.setThreadNamePattern(newNamePattern);
    }

    public String getName() {
        return name;
    }

    public String getNewNamePattern() {
        return newNamePattern;
    }

    private UpdateFailedException notConfigured() {
        return new UpdateFailedException("No thread factory named " + name + " is configured");
    }
}
