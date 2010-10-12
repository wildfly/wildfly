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

/**
 * @author Emanuel Muckenhuber
 */
public class HostPathAdd extends AbstractHostModelUpdate<Void> {

    private static final long serialVersionUID = 1487178917602420851L;
    private final PathElementUpdate update;

    public HostPathAdd(PathElementUpdate update) {
        if(update == null) {
            throw new IllegalArgumentException("null path element update");
        }
        if(update.getPath() == null) {
            throw new IllegalArgumentException("null path for path element " + update.getName());
        }
        this.update = update;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(HostModel element) throws UpdateFailedException {
        final PathElement path = element.addPath(update.getName());
        if(path == null) {
            throw new UpdateFailedException("duplicate path " + update.getName());
        }
        update.applyUpdate(path);
    }

    /** {@inheritDoc} */
    public AbstractHostModelUpdate<?> getCompensatingUpdate(HostModel original) {
        return new HostPathRemove(update.getName());
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerPathAdd(update);
    }

}
