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

import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.msc.service.ServiceController;

/**
 * Update to the {@code ServerModel} to remove a {@code PathElement}
 *
 * @author Emanuel Muckenhuber
 */
public class ServerPathRemove extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = 6683732173045719685L;
    private final String name;

    public ServerPathRemove(String name) {
        super(true, false);
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        if(! element.removePath(name)) {
            throw new UpdateFailedException(String.format("path (%s) does not exist.", name));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        final PathElement element = original.getPath(name);
        if(element == null) {
            return null;
        }
        final PathElementUpdate update = new PathElementUpdate(name, element.getPath(), element.getRelativeTo());
        return new ServerPathAdd(update);
    }

    @Override
    public <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceController<?> controller = context.getServiceRegistry().getService(AbstractPathService.pathNameOf(name));
        if(controller == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
    }

}
