/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.lang.reflect.Method;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * A simple message endpoint factory
 *
 * @author <a href="mailto:vrastsel@redhat.com>Vladimir Rastseluev</a>
 */
public class ValidMessageEndpointFactory implements MessageEndpointFactory {

    private MessageEndpoint me;

    /**
     * Constructor
     *
     * @param me The message endpoint that should be used
     */
    public ValidMessageEndpointFactory(MessageEndpoint me) {
        this.me = me;
    }

    /**
     * {@inheritDoc}
     */
    public MessageEndpoint createEndpoint(XAResource xaResource) {
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) {
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeliveryTransacted(Method method) {
        return false;
    }

    @Override
    public String getActivationName() {
        return "activationName";
    }

    @Override
    public Class<?> getEndpointClass() {
        return me.getClass();
    }
}
