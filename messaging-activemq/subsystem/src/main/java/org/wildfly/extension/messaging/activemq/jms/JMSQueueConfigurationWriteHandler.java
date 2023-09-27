/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;

/**
 * Write attribute handler for attributes that update the persistent configuration of a Jakarta Messaging queue resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSQueueConfigurationWriteHandler extends ReloadRequiredWriteAttributeHandler {

    public static final JMSQueueConfigurationWriteHandler INSTANCE = new JMSQueueConfigurationWriteHandler();

    private JMSQueueConfigurationWriteHandler() {
        super(JMSQueueDefinition.ATTRIBUTES);
    }
}
