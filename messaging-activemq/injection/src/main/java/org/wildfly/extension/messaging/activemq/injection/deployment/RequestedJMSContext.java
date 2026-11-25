/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;

/**
 * Injection of JMSContext in the @RequestScoped scope.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
@RequestScoped
class RequestedJMSContext extends AbstractJMSContext {

    @PreDestroy
    @Override
    void cleanUp() {
        super.cleanUp();
    }
}
