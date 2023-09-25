/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.apache.activemq.artemis.api.jms.JMSFactoryType.CF;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.QUEUE_CF;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.QUEUE_XA_CF;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.TOPIC_CF;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.TOPIC_XA_CF;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.XA_CF;

import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;

/**
 * Connection factory type enumeration and their respective value in ActiveMQ Artemis Jakarta Messaging API
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public enum ConnectionFactoryType {
    GENERIC(CF),
    TOPIC(TOPIC_CF),
    QUEUE(QUEUE_CF),
    XA_GENERIC(XA_CF),
    XA_QUEUE(QUEUE_XA_CF),
    XA_TOPIC(TOPIC_XA_CF);

    private final JMSFactoryType type;

    ConnectionFactoryType(JMSFactoryType type) {
        this.type = type;
    }

    public JMSFactoryType getType() {
        return type;
    }

    public static final ParameterValidator VALIDATOR = EnumValidator.create(ConnectionFactoryType.class);
}
