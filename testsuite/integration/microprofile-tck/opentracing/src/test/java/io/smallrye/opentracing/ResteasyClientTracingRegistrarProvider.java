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
package io.smallrye.opentracing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;

/**
 * @author Pavol Loffay
 */
public class ResteasyClientTracingRegistrarProvider implements ClientTracingRegistrarProvider {

    @Override
    public ClientBuilder configure(ClientBuilder clientBuilder) {
        // Make sure executor is the same as a default in resteasy ClientBuilder
        return configure(clientBuilder, Executors.newFixedThreadPool(10));
    }

    @Override
    public ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService) {
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) clientBuilder;
        Tracer tracer = CDI.current().select(Tracer.class).get();
        return resteasyClientBuilder.executorService(new TracedExecutorService(executorService, tracer)).register(
                new SmallRyeClientTracingFeature(tracer));
    }
}
