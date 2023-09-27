/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.wildfly.extension.messaging.activemq.CommonAttributes;

/**
 * Write attribute handler for attributes that update the persistent configuration of a Jakarta Messaging topic resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicConfigurationWriteHandler extends ReloadRequiredWriteAttributeHandler {

    public static final JMSTopicConfigurationWriteHandler INSTANCE = new JMSTopicConfigurationWriteHandler();

    private JMSTopicConfigurationWriteHandler() {
        super(CommonAttributes.DESTINATION_ENTRIES);
    }
}
