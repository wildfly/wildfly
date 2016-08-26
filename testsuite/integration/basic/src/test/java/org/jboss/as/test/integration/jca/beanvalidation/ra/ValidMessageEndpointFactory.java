/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.lang.reflect.Method;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
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
