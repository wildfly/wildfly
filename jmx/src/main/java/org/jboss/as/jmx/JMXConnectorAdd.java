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

package org.jboss.as.jmx;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;

/**
 * @author Emanuel Muckenhuber
 */
public class JMXConnectorAdd extends AbstractSubsystemUpdate<JmxSubsystemElement, Void> {

    private static final long serialVersionUID = -1898250436998656765L;
    private String serverBinding;
    private String registryBinding;

    public JMXConnectorAdd(final String serverBinding, final String registryBinding) {
        super(Namespace.CURRENT.getUriString());
        if(serverBinding == null) {
            throw new IllegalArgumentException("null connector binding");
        }
        if(registryBinding == null) {
            throw new IllegalArgumentException("null registry binding");
        }
        this.serverBinding = serverBinding;
        this.registryBinding = registryBinding;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();
        JMXConnectorService.addService(builder, serverBinding, registryBinding)
            .addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JmxSubsystemElement, ?> getCompensatingUpdate(JmxSubsystemElement original) {
        return new JMXConnectorRemove();
    }

    /** {@inheritDoc} */
    public Class<JmxSubsystemElement> getModelElementType() {
        return JmxSubsystemElement.class;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JmxSubsystemElement element) throws UpdateFailedException {
        element.setConnector(new JMXConnectorElement(serverBinding, registryBinding));
    }

}
