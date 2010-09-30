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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistryException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadFactoryAdd extends AbstractThreadsSubsystemUpdate<Void> {

    private static final long serialVersionUID = 1935521612404501853L;

    private final Map<String, String> properties = new HashMap<String, String>();

    private final String name;

    private String groupName;
    private String threadNamePattern;
    private Integer priority;

    public ThreadFactoryAdd(final String name) {
        super(false);
        this.name = name;
    }

    public ThreadFactoryRemove getCompensatingUpdate(final ThreadsSubsystemElement original) {
        return new ThreadFactoryRemove(name);
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        final ThreadFactoryService service = new ThreadFactoryService();
        service.setNamePattern(threadNamePattern);
        service.setPriority(priority);
        service.setThreadGroupName(groupName);
        final UpdateResultHandler.ServiceStartListener<P> listener = new UpdateResultHandler.ServiceStartListener<P>(handler, param);
        final BatchBuilder batchBuilder = updateContext.getBatchBuilder();
        final BatchServiceBuilder<ThreadFactory> builder = batchBuilder.addService(Services.threadFactoryName(name), service);
        builder.addListener(listener);
        builder.setInitialMode(ServiceController.Mode.IMMEDIATE);
        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            handler.handleFailure(e, param);
        }
    }

    protected void applyUpdate(final ThreadsSubsystemElement element) throws UpdateFailedException {
        final ThreadFactoryElement addedElement = element.addThreadFactory(name);
        if (addedElement == null) {
            throw new UpdateFailedException("A thread factory named '" + name + "' is already registered here");
        }
        if (groupName != null) addedElement.setGroupName(groupName);
        if (threadNamePattern != null) addedElement.setThreadNamePattern(threadNamePattern);
        if (priority != null) addedElement.setPriority(priority);
        if (! properties.isEmpty()) addedElement.setProperties(properties);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    public String getThreadNamePattern() {
        return threadNamePattern;
    }

    public void setThreadNamePattern(final String threadNamePattern) {
        this.threadNamePattern = threadNamePattern;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
