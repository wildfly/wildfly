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
import io.opentracing.contrib.jaxrs2.server.OperationNameProvider;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;

import javax.servlet.ServletContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class TracerDynamicFeature implements DynamicFeature {
    @Context
    ServletContext servletContext;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Tracer tracer;

        Object tracerObject = servletContext.getAttribute(TracerInitializer.SMALLRYE_OPENTRACING_TRACER);
        if (tracerObject instanceof Tracer) {
            tracer = (Tracer) tracerObject;
        } else {
            // should never happen, but if it does, there's something really wrong
            // we log a warn-level message here then
            TracingLogger.ROOT_LOGGER.noTracerAvailable();
            return;
        }

        ServerTracingDynamicFeature delegate = new ServerTracingDynamicFeature.Builder(tracer)
                .withOperationNameProvider(OperationNameProvider.ClassNameOperationName.newBuilder())
                .withTraceSerialization(false)
                .build();

        delegate.configure(resourceInfo, context);
    }

}
