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

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.EnumSet;

@WebListener
public class TracerInitializer implements ServletContextListener {
    public static final String SMALLRYE_OPENTRACING_SERVICE_NAME = "smallrye.opentracing.serviceName";
    public static final String SMALLRYE_OPENTRACING_TRACER = "smallrye.opentracing.tracer";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (GlobalTracer.isRegistered()) {
            TracingLogger.ROOT_LOGGER.alreadyRegistered();
            return;
        }

        // an application has the option to provide a TracerFactory
        Tracer tracer = TracerResolver.resolveTracer();
        if (null == tracer) {
            String serviceName = sce.getServletContext().getInitParameter(SMALLRYE_OPENTRACING_SERVICE_NAME);
            if (null == serviceName || serviceName.isEmpty()) {
                // this should really not happen, as this is set by the deployment processor
                TracingLogger.ROOT_LOGGER.noServiceName();
                tracer = NoopTracerFactory.create();
            } else {
                tracer = Configuration.fromEnv(serviceName).getTracer();
            }
        }

        TracingLogger.ROOT_LOGGER.registeringTracer(tracer.getClass().getName());
        sce.getServletContext().setAttribute(SMALLRYE_OPENTRACING_TRACER, tracer);
        addJaxRsIntegration(sce.getServletContext(), tracer);

        TracingLogger.ROOT_LOGGER.initializing(tracer.toString());
    }

    private void addJaxRsIntegration(ServletContext servletContext, Tracer tracer) {
        servletContext.setInitParameter("resteasy.providers", TracerDynamicFeature.class.getName());
        FilterRegistration.Dynamic filterRegistration = servletContext.addFilter(SpanFinishingFilter.class.getName(),
                new SpanFinishingFilter(tracer));
        filterRegistration.setAsyncSupported(true);
        filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "*");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
