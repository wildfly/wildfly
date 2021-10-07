/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry.deployment;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYOTELEXT", length = 4)
public interface OpenTelemetryExtensionLogger extends BasicLogger {
    OpenTelemetryExtensionLogger OTEL_LOGGER = Logger.getMessageLogger(OpenTelemetryExtensionLogger.class,
            OpenTelemetryExtensionLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating OpenTelemetry Subsystem")
    void activatingSubsystem();

    @LogMessage(level = DEBUG)
    @Message(id = 2, value = "OpenTelemetry Subsystem is processing deployment")
    void processingDeployment();

    @LogMessage(level = DEBUG)
    @Message(id = 3, value = "The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping OpenTelemetry integration.")
    void noCdiDeployment();

    @Message(id = 4, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @LogMessage(level = ERROR)
    @Message(id = 5, value = "Error resolving the OpenTelemetry instance.")
    void errorResolvingTelemetry(@Cause Exception ex);

    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "Deriving service name based on the deployment unit's name: %s")
    void serviceNameDerivedFromDeploymentUnit(String serviceName);

    @LogMessage(level = DEBUG)
    @Message(id = 7, value = "Registering %s as the OpenTelemetry Tracer")
    void registeringTracer(String message);

    @Message(id = 8, value = "An unsupported exporter was specified: '%s'.")
    IllegalArgumentException unsupportedExporter(String exporterType);

    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Error resolving the tracer.")
    void errorResolvingTracer(@Cause Exception ex);

    @Message(id = 10, value = "An unsupported span processor was specified: '%s'.")
    IllegalArgumentException unsupportedSpanProcessor(String spanProcessor);

    @Message(id = 11, value = "Unrecognized value for sampler: '%s'.")
    IllegalArgumentException unsupportedSampler(String sampler);

    @Message(id = 12, value = "Invalid ratio. Must be between 0.0 and 1.0 inclusive")
    IllegalArgumentException invalidRatio();
}
