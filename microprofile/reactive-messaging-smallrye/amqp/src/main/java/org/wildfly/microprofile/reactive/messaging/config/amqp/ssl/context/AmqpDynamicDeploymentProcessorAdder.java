/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.wildfly.microprofile.reactive.messaging.config.amqp.ssl.context;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.microprofile.reactive.messaging.common.DynamicDeploymentProcessorAdder;
/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AmqpDynamicDeploymentProcessorAdder implements DynamicDeploymentProcessorAdder {

    private static final String VERTX_DISABLE_DNS_RESOLVER = "vertx.disableDnsResolver";

    @Override
    public void addDeploymentProcessor(DeploymentProcessorTarget target, String subsystemName) {

        final int POST_MODULE_MICROPROFILE_REACTIVE_MESSAGING = 0x3828;

        // We need to disable vertx's DNS resolver as it causes failures under k8s
        if (System.getProperty(VERTX_DISABLE_DNS_RESOLVER) == null) {
            System.setProperty(VERTX_DISABLE_DNS_RESOLVER, "true");
        }

        target.addDeploymentProcessor(subsystemName,
                Phase.POST_MODULE,
                POST_MODULE_MICROPROFILE_REACTIVE_MESSAGING,
                new AmqpReactiveMessagingSslConfigProcessor());
    }
}
