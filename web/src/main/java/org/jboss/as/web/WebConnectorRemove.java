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

package org.jboss.as.web;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * Update removing a web connector
 *
 * @author Emanuel Muckenhuber
 */
public class WebConnectorRemove extends AbstractWebSubsystemUpdate<Void> {

    private static final long serialVersionUID = -5287654247889240561L;
    private final String name;

    public WebConnectorRemove(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null connector name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(WebSubsystemElement element) throws UpdateFailedException {
        if(! element.removeConnector(name)) {
            throw new UpdateFailedException("no such connector " + name);
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service = context.getServiceContainer().getService(WebSubsystemElement.JBOSS_WEB_CONNECTOR.append(name));
        if(service == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<WebSubsystemElement, ?> getCompensatingUpdate(WebSubsystemElement original) {
        final WebConnectorElement connector = original.getConnector(name);
        if(connector == null) {
            return null;
        }
        final WebConnectorAdd action = new WebConnectorAdd(name);
        action.setBindingRef(connector.getBindingRef());
        action.setProtocol(connector.getProtocol());
        action.setScheme(connector.getScheme());
        action.setExecutorRef(connector.getExecutorRef());
        action.setEnabled(connector.isEnabled());
        action.setRedirectPort(connector.getRedirectPort());
        action.setProxyName(connector.getProxyName());
        action.setProxyPort(connector.getProxyPort());
        action.setEnableLookups(connector.isEnableLookups());
        action.setMaxPostSize(connector.getMaxPostSize());
        action.setMaxSavePostSize(connector.getMaxSavePostSize());
        action.setSecure(connector.isSecure());
        return action;
    }

}
