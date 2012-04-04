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

package org.jboss.as.server;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Exposes the {@link ServerEnvironment} via a {@link Service}.
 * <p>
 * Services that need access to the {@code ServerEnvironment} can use this service to
 * have it injected. For example, suppose we have a service {@code MyService}
 * that has a field {@code injectedEnvironment} into which it wants the
 * ServerEnvironment injected. And suppose {@code MyService} exposes a utility method
 * to facilitate installing it via a {@link ServiceTarget}. The {@code ServerEnvironment}
 * injection could be done as follows:
 * </p>
 *
 * <pre>
 * public static void addService(BatchBuilder batchBuilder) {
 *     MyService myService = new MyService();
 *     InjectedValue<ServerEnvironment> injectedEnvironment = myService.injectedEnvironment;
 *
 *     batchBuilder.addService(MyService.SERVICE_NAME, myService)
 *                 .addSystemDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, injectedEnvironment);
 * }
 * </pre>
 *
 * @author Brian Stansberry
 */
public class ServerEnvironmentService implements Service<ServerEnvironment> {

    /** Standard ServiceName under which a ServerEnvironmentService would be registered */
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "environment");

    /**
     * Adds a ServerEnvironmentService based on the given {@code serverEnvironment}
     * to the given batch under name {@link #SERVICE_NAME}.
     *
     * @param serverEnvironment the {@code ServerEnvironment}. Cannot be {@code null}
     * @param target the batch builder. Cannot be {@code null}
     */
    public static void addService(ServerEnvironment serverEnvironment, ServiceTarget target) {
        target.addService(SERVICE_NAME, new ServerEnvironmentService(serverEnvironment))
            .install();
    }

    private final ServerEnvironment serverEnvironment;

    /**
     * Creates a ServerEnvironmentService that uses the given {@code serverEnvironment}
     * as its {@link #getValue() value}.
     *
     * @param serverEnvironment the {@code ServerEnvironment}. Cannot be {@code null}
     */
    ServerEnvironmentService(ServerEnvironment serverEnvironment) {
        assert serverEnvironment != null : "serverEnvironment is null";
        this.serverEnvironment = serverEnvironment;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // no-op
    }

    @Override
    public void stop(StopContext context) {
        // no-op
    }

    @Override
    public ServerEnvironment getValue() throws IllegalStateException {
        return serverEnvironment;
    }

}
