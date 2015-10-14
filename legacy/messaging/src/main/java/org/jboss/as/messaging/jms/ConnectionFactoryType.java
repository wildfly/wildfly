/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;

/**
 * Connection factory type enumeration and their respective value in HornetQ JMS API
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public enum ConnectionFactoryType {
    GENERIC(JMSFactoryType.CF),
    TOPIC(JMSFactoryType.TOPIC_CF),
    QUEUE(JMSFactoryType.QUEUE_CF),
    XA_GENERIC(JMSFactoryType.XA_CF),
    XA_QUEUE(JMSFactoryType.QUEUE_XA_CF),
    XA_TOPIC(JMSFactoryType.TOPIC_XA_CF);

    private final JMSFactoryType type;

    ConnectionFactoryType(JMSFactoryType type) {
        this.type = type;
    }

    public JMSFactoryType getType() {
        return type;
    }

    public static final ParameterValidator VALIDATOR = new EnumValidator<>(ConnectionFactoryType.class, true, false);

    // copied from HornetQ to avoid import HornetQ artifacts just to define attribute constants and enum validator
    private enum JMSFactoryType
    {
        CF, QUEUE_CF, TOPIC_CF, XA_CF, QUEUE_XA_CF, TOPIC_XA_CF;
    }
}
