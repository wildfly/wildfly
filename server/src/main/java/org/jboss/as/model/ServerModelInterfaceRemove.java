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

package org.jboss.as.model;

import org.jboss.as.model.socket.InterfaceAdd;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.service.ServiceController;

/**
 * Update removing a {@link InterfaceElement} from the {@link ServerModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerModelInterfaceRemove extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = -4178671085310775352L;
    private final String interfaceName;

    public ServerModelInterfaceRemove(final String name) {
        this.interfaceName = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        if(element.removeInterface(interfaceName)) {
            throw new UpdateFailedException(String.format("network interface (%s) not found", interfaceName));
        }
    }

    /** {@inheritDoc} */
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        final InterfaceElement element = original.getInterface(interfaceName);
        if(element == null) {
            return null;
        }
        return new ServerModelInterfaceAdd(new InterfaceAdd(element));
    }

    public <P> void applyUpdate(UpdateContext updateContext, org.jboss.as.model.UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceController<?> controller = updateContext.getServiceRegistry().getService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName));
        if(controller == null) {
            resultHandler.handleSuccess(null, param);
            return;
        }
        controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
    }

}
