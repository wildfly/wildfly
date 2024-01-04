/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.dwm;

import org.jboss.jca.core.spi.workmanager.policy.Policy;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Connection validator service
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
final class PolicyService implements Service<Policy> {

    private final Policy policy;
    /**
     * Constructor
     */
    public PolicyService(Policy  policy) {
        this.policy = policy;
    }

    @Override
    public Policy getValue() throws IllegalStateException {
        return policy;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }
}
