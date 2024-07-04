/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.opentracing;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYTRACEXT", length = 4)
public interface TracingExtensionLogger extends BasicLogger {
    TracingExtensionLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), TracingExtensionLogger.class, TracingExtensionLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile OpenTracing Subsystem")
    void activatingSubsystem();

    /*
    // no longer used

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
    */

    //9, 10 and 11 are taken downstream
    /*
    @Message(id = 9, value = "")
    OperationFailedException seeDownstream();

    @Message(id = 10, value = "")
    String seeDownstream();

    @Message(id = 11, value = "")
    OperationFailedException seeDownstream();
    */

    // no longer used
    // @LogMessage(level = WARN)
    // @Message(id = 12, value="No Jaeger endpoint or sender-binding configured. Installing a no-op sender")
    // void senderNotConfigured();

    @Message(id = 13, value = "The migrate operation cannot be performed: the server must be in admin-only mode")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @Message(id = 14, value = "Migration failed. See results for more details.")
    String migrationFailed();
}
