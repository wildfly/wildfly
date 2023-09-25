/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;

/**
 * Write attribute handler for attributes that update the persistent configuration of a core queue.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueConfigurationWriteHandler extends ReloadRequiredWriteAttributeHandler {

    public static final QueueConfigurationWriteHandler INSTANCE = new QueueConfigurationWriteHandler();

    private QueueConfigurationWriteHandler() {
        super(QueueDefinition.ATTRIBUTES);
    }
}
