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
package org.jboss.as.test.integration.microprofile.opentracing.application;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Tracer;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@ApplicationPath("service-endpoint")
public class TracerIdentityApplication extends Application {

    @Path("/app")
    public static class TestResource {

        @Inject
        private Tracer tracer;

        @GET
        @Produces("text/plain")
        public String get() {
            JaegerTracer jTracer = (JaegerTracer) tracer;
            return Integer.toString(System.identityHashCode(jTracer));
        }
    }
}
