/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.lang.reflect.Method;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.resource.spi.endpoint.MessageEndpoint;


/**
 * A simple message endpoint
 *
 * @author <a href="mailto:vrastsel@redhat.com>Vladimir Rastseluev</a>
 */
public class ValidMessageEndpoint implements MessageEndpoint, MessageListener {


    private Message message;

    /**
     * Constructor
     */
    public ValidMessageEndpoint() {
    }

    /**
     * {@inheritDoc}
     */
    public void onMessage(Message message) {
        this.message = message;

    }

    /**
     * Get the message
     *
     * @return The value
     */
    public Message getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    public void afterDelivery() {
    }

    /**
     * {@inheritDoc}
     */
    public void beforeDelivery(Method method) {
    }

    /**
     * {@inheritDoc}
     */
    public void release() {
    }
}
