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

import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.msc.service.ServiceTarget;

/**
 * Update to the {@code ServerModel} to add a new {@code PathElement}
 *
 * @author Emanuel Muckenhuber
 */
public class ServerPathAdd extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = -7255447870952892481L;
    private final PathElementUpdate update;

    public ServerPathAdd(PathElementUpdate update) {
        if(update == null) {
            throw new IllegalArgumentException("null path element update");
        }
        if(update.getPath() == null) {
            throw new IllegalArgumentException("null path for path element " + update.getName());
        }
        this.update = update;
    }

    protected ServerPathAdd(final PathElement element) {
        if(element.getPath() == null) {
            throw new IllegalArgumentException("null path for path element " + element.getName());
        }
        this.update = new PathElementUpdate(element.getName(), element.getPath(), element.getRelativeTo());
    }

    /** {@inheritDoc} */
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        final PathElement pathElement = element.addPath(update.getName());
        if(pathElement == null) {
            throw new UpdateFailedException("duplicate path definition " + update.getName());
        }
        update.applyUpdate(pathElement);
    }

    /** {@inheritDoc} */
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        return new ServerPathRemove(update.getName());
    }

    /** {@inheritDoc} */
    public <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceTarget target = context.getServiceTarget().subTarget();
        target.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
        createService(target);
    }

    void createService(final ServiceTarget target) {
        if(update.isAbsolutePath()) {
            AbsolutePathService.addService(update.getName(), update.getPath(), target);
        } else {
            RelativePathService.addService(update.getName(), update.getPath(), update.getRelativeTo(), target);
        }
    }

}
