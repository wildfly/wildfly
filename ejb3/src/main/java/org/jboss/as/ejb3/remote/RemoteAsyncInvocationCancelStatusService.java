/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of cancellation status of remote asynchronous method invocations on EJBs.
 *
 * @author Jaikiran Pai
 */
public class RemoteAsyncInvocationCancelStatusService implements Service<RemoteAsyncInvocationCancelStatusService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "remote-async-invocation-cancellation-tracker");

    private final Map<Short, CancellationFlag> asyncInvocations = new HashMap<Short, CancellationFlag>();

    public synchronized void registerAsyncInvocation(final short invocationId, final CancellationFlag cancellationFlag) {
        this.asyncInvocations.put(invocationId, cancellationFlag);
    }

    public synchronized CancellationFlag asyncInvocationDone(final short invocationId) {
        return this.asyncInvocations.remove(invocationId);
    }

    public synchronized CancellationFlag getCancelStatus(final short invocationId) {
        return this.asyncInvocations.get(invocationId);
    }

    @Override
    public void start(StartContext startContext) throws StartException {

    }

    @Override
    public void stop(StopContext stopContext) {
        synchronized (this.asyncInvocations) {
            this.asyncInvocations.clear();
        }
    }

    @Override
    public RemoteAsyncInvocationCancelStatusService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
