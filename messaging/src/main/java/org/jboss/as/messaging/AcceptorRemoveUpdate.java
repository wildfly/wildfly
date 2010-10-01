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

package org.jboss.as.messaging;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Update removing an acceptor {@code TransportConfiguration}.
 *
 * @author Emanuel Muckenhuber
 */
public class AcceptorRemoveUpdate extends AbstractMessagingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 107065576462327213L;

    private final String name;

    public AcceptorRemoveUpdate(String name) {
        super();
        this.name = name;
    }

    /** {@inheritDoc} */
    void applyUpdate(Configuration configuration) throws UpdateFailedException {
        final TransportConfiguration acceptor = getAcceptorConfig(name, configuration);
        if(acceptor == null) {
            throw new UpdateFailedException("acceptor configuration not found: " + name);
        }
        configuration.getAcceptorConfigurations().remove(acceptor);
    }

    /** {@inheritDoc} */
    AbstractSubsystemUpdate<MessagingSubsystemElement, ?> getCompensatingUpdate(Configuration original) {
        final TransportConfiguration acceptor = getAcceptorConfig(name, original);
        if(acceptor == null) {
            throw new IllegalStateException("acceptor configuration not found: " + name);
        }
        return new AcceptorAddUpdate(acceptor);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub

    }

    protected static TransportConfiguration getAcceptorConfig(String name, Configuration configuration) {
        for(final TransportConfiguration acceptor : configuration.getAcceptorConfigurations()) {
            if(acceptor.getName().equals(name)) {
                return acceptor;
            }
        }
        return null;
    }

}
