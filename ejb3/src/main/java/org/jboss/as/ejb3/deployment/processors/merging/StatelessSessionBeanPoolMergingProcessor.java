/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;

/**
 * Sets up the stateless bean component description with the pool name configured for the bean via the {@link org.jboss.ejb3.annotation.Pool}
 * annotation and/or the deployment descriptor
 *
 * @author Jaikiran Pai
 */
public class StatelessSessionBeanPoolMergingProcessor extends AbstractPoolMergingProcessor<StatelessComponentDescription> {

    public StatelessSessionBeanPoolMergingProcessor() {
        super(StatelessComponentDescription.class);

    }

    @Override
    protected void setPoolName(final StatelessComponentDescription componentDescription, final String poolName) {
        componentDescription.setPoolConfigName(poolName);
    }
}
