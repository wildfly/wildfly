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

package org.jboss.as.remoting;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Add a connector to a remoting container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public final class AddConnectorUpdate extends AbstractRemotingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -1238913680118311381L;

    private final String name;
    private final String socketBinding;
    private SaslElement saslElement;
    private String authenticationProvider;
    private final Map<String, String> properties = new HashMap<String, String>();

    public AddConnectorUpdate(String name, String socketBinding) {
        this.name = name;
        this.socketBinding = socketBinding;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<RemotingSubsystemElement, ?> getCompensatingUpdate(RemotingSubsystemElement original) {
        return new RemoveConnectorUpdate(name);
    }

    /** {@inheritDoc} */
    protected void applyUpdate(RemotingSubsystemElement element) throws UpdateFailedException {
        final ConnectorElement connector = element.addConnector(name, socketBinding);
        connector.setAuthenticationProvider(authenticationProvider);
        connector.setSaslElement(saslElement);
        connector.setConnectorProperties(properties);
    }

    public void setSaslElement(SaslElement saslElement) {
        this.saslElement = saslElement;
    }

    public void setAuthenticationProvider(String authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
