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

package org.jboss.as.model.socket;

import org.jboss.as.model.AbstractSocketBindingUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.msc.service.ServiceController;


/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingRemove extends AbstractSocketBindingUpdate {

    private static final long serialVersionUID = -4506224971076132988L;
    private final String name;

    public SocketBindingRemove(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    public AbstractSocketBindingUpdate getCompensatingUpdate(SocketBindingGroupElement original) {
        final SocketBindingElement element = original.getSocketBinding(name);
        return new SocketBindingAdd(element);
    }

    /** {@inheritDoc} */
    protected void applyUpdate(SocketBindingGroupElement element) throws UpdateFailedException {
        element.removeSocketBinding(name);
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceController<?> controller = updateContext.getServiceContainer().getService(SocketBinding.JBOSS_BINDING_NAME.append(name));
        if(controller == null) {
            resultHandler.handleSuccess(null, param);
            return;
        }
        controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
    }

}
