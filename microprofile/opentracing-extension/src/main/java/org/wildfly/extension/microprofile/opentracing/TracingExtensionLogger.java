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

package org.wildfly.extension.microprofile.opentracing;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.annotations.Cause;

@MessageLogger(projectCode = "WFLYTRACEXT", length = 4)
public interface TracingExtensionLogger extends BasicLogger {
    TracingExtensionLogger ROOT_LOGGER = Logger.getMessageLogger(TracingExtensionLogger.class, TracingExtensionLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile OpenTracing Subsystem")
    void activatingSubsystem();

    @LogMessage(level = DEBUG)
    @Message(id = 2, value = "MicroProfile OpenTracing Subsystem is processing deployment")
    void processingDeployment();

    @LogMessage(level = DEBUG)
    @Message(id = 3, value = "The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping MicroProfile OpenTracing integration.")
    void noCdiDeployment();

    @LogMessage(level = DEBUG)
    @Message(id = 4, value = "Deriving service name based on the deployment unit's name: %s")
    void serviceNameDerivedFromDeploymentUnit(String serviceName);

    @LogMessage(level = DEBUG)
    @Message(id = 5, value = "Registering the TracerInitializer filter")
    void registeringTracerInitializer();

    @Message(id = 6, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    String deploymentRequiresCapability(String deploymentName, String capabilityName);

    @LogMessage(level = DEBUG)
    @Message(id = 7, value = "No module found for deployment %s for resolving the tracer.")
    void tracerResolverDeployementModuleNotFound(String deployment);

    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Error using tracer resolver to resolve the tracer.")
    void errorResolvingTracer(@Cause Exception ex);

    @Message(id = 9, value = "Unknown metric %s")
    String unknownMetric(Object metric);

    @Message(id = 10, value = "Tracer is not a managed Jaeger tracer")
    String notManagedJaegerTracer();
}
