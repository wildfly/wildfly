/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.common;

import org.jboss.as.server.DeploymentProcessorTarget;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface DynamicDeploymentProcessorAdder {
    void addDeploymentProcessor(DeploymentProcessorTarget target, String subsystemName);
}
