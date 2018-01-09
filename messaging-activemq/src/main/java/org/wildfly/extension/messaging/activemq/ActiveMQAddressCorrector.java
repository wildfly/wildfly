/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.ParameterCorrector;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class ActiveMQAddressCorrector {

    private static final String JMS_QUEUE_ADDRESS_PREFIX = "jms.queue.";
    private static final String JMS_TOPIC_ADDRESS_PREFIX = "jms.topic.";

    static final ParameterCorrector CORRECTOR = (newValue, currentValue) -> {
        if (newValue.isDefined()) {
            String newValueStr = newValue.asString();
            String correctedValue = null;
            if (newValueStr.equals("jms")) {
                correctedValue = "";
            } else if (newValueStr.startsWith(JMS_QUEUE_ADDRESS_PREFIX)) {
                correctedValue = newValueStr.substring(JMS_QUEUE_ADDRESS_PREFIX.length());
            } else if (newValueStr.startsWith(JMS_TOPIC_ADDRESS_PREFIX)) {
                correctedValue = newValueStr.substring(JMS_TOPIC_ADDRESS_PREFIX.length());
            }

            if (correctedValue != null) {
                MessagingLogger.ROOT_LOGGER.convertingActiveMQAddress(newValueStr, correctedValue);
                return new ModelNode(correctedValue);
            }

        }
        return newValue;
    };
}
