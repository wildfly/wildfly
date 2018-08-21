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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

@MessageLogger(projectCode = "WFLYTRAC", length = 4)
public interface TracingLogger extends BasicLogger {
    TracingLogger ROOT_LOGGER = Logger.getMessageLogger(TracingLogger.class, TracingLogger.class.getPackage().getName());

    @LogMessage(level = DEBUG)
    @Message(id = 1, value = "Tracer initialized: %s")
    void initializing(String message);

    @LogMessage(level = DEBUG)
    @Message(id = 2, value = "A Tracer is already registered at the GlobalTracer. Skipping resolution.")
    void alreadyRegistered();

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Could not determine the service name and can't therefore use Jaeger Tracer. Using NoopTracer.")
    void noServiceName();

    @LogMessage(level = DEBUG)
    @Message(id = 4, value = "Registering %s as the OpenTracing Tracer")
    void registeringTracer(String message);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "No tracer available to JAX-RS. Skipping MicroProfile OpenTracing configuration for JAX-RS")
    void noTracerAvailable();

    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "Extra Tracer bean found: %s. Vetoing it, please use TracerResolver to specify a custom tracer to use.")
    void extraTracerBean(String clazz);
}
