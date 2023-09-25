/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;

/**
 * Sets up the component description for a MDB with the pool name configured via {@link org.jboss.ejb3.annotation.Pool}
 * annotation and/or the deployment descriptor
 *
 * @author Jaikiran Pai
 */
public class MessageDrivenBeanPoolMergingProcessor extends AbstractPoolMergingProcessor<MessageDrivenComponentDescription> {

    public MessageDrivenBeanPoolMergingProcessor() {
        super(MessageDrivenComponentDescription.class);
    }

    @Override
    protected void setPoolName(final MessageDrivenComponentDescription componentDescription, final String poolName) {
        componentDescription.setPoolConfigName(poolName);
    }
}
