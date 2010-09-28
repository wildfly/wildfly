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

import java.util.Map;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadFactoryRemove extends AbstractThreadsSubsystemUpdate<Void> {

    private static final long serialVersionUID = -6232099966839106128L;

    private final String name;

    public ThreadFactoryRemove(final String name) {
        super(false);
        this.name = name;
    }

    public ThreadFactoryAdd getCompensatingUpdate(final ThreadsSubsystemElement original) {
        final ThreadFactoryAdd add = new ThreadFactoryAdd(name);
        final ThreadFactoryElement threadFactory = original.getThreadFactory(name);
        final Integer priority = threadFactory.getPriority();
        if (priority != null) add.setPriority(priority);
        final String groupName = threadFactory.getGroupName();
        if (groupName != null) add.setGroupName(groupName);
        final String namePattern = threadFactory.getThreadNamePattern();
        if (namePattern != null) add.setThreadNamePattern(namePattern);
        final Map<String,String> properties = threadFactory.getProperties();
        if (! properties.isEmpty()) add.getProperties().putAll(properties);
        return add;
    }

    protected <P> void applyUpdate(final ServiceContainer container, final UpdateResultHandler<? super Void, P> handler, final P param) {
        final ServiceController<?> controller = container.getService(Services.threadFactoryName(name));
        if (controller == null) {
            handler.handleSuccess(null, param);
        } else {
            controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(handler, param));
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    protected void applyUpdate(final ThreadsSubsystemElement element) throws UpdateFailedException {
        element.removeThreadFactory(name);
    }

    public String getName() {
        return name;
    }
}
