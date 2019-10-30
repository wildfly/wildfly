/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.microprofile.opentracing.smallrye;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.smallrye.opentracing.SmallRyeClientTracingFeature;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WildFlyClientTracingRegistrarProvider implements ClientTracingRegistrarProvider {
    @Override
    public ClientBuilder configure(ClientBuilder clientBuilder) {
        ExecutorService executorService;

        try {
            executorService = InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
        } catch (NamingException e) {
            executorService = Executors.newFixedThreadPool(10);
        }

        return configure(clientBuilder, executorService);
    }

    @Override
    public ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService) {
        Tracer tracer = CDI.current().select(Tracer.class).get();

        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) clientBuilder;
        return resteasyClientBuilder
                .asyncExecutor(new TracedExecutorService(executorService, tracer))
                .register(new SmallRyeClientTracingFeature(tracer));
    }
}
