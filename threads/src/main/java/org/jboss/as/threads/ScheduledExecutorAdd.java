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

import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceContainer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ScheduledExecutorAdd extends AbstractThreadsSubsystemUpdate<Void> {

    private static final long serialVersionUID = 5228895749512255692L;

    private final String name;
    private final ScaledCount maxSize;

    public ScheduledExecutorAdd(final String name, final ScaledCount maxSize) {
        this.name = name;
        this.maxSize = maxSize;
    }

    public AbstractThreadsSubsystemUpdate<?> getCompensatingUpdate(final ThreadsSubsystemElement original) {
        return null;
    }

    protected <P> void applyUpdate(final ServiceContainer container, final UpdateResultHandler<? super Void, P> handler, final P param) {
    }

    protected void applyUpdate(final ThreadsSubsystemElement element) throws UpdateFailedException {
    }
}
