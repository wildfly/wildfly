/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.microprofile.reactive.messaging.common.DynamicDeploymentProcessorAdder;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KafkaDynamicDeploymentProcessorAdder implements DynamicDeploymentProcessorAdder {
    @Override
    public void addDeploymentProcessor(DeploymentProcessorTarget target, String subsystemName) {

        final int POST_MODULE_MICROPROFILE_REACTIVE_MESSAGING = 0x3828;

        target.addDeploymentProcessor(subsystemName,
                Phase.POST_MODULE,
                POST_MODULE_MICROPROFILE_REACTIVE_MESSAGING,
                new ReactiveMessagingSslConfigProcessor());
    }
}
