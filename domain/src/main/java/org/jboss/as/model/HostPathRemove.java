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
public class HostPathRemove extends AbstractHostModelUpdate<Void> {

    private static final long serialVersionUID = 8637955118179300666L;
    private final String name;

    public HostPathRemove(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(HostModel element) throws UpdateFailedException {
        if(! element.removePath(name)) {
            throw new UpdateFailedException(String.format("path (%s) does not exist", name));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbstractHostModelUpdate<?> getCompensatingUpdate(HostModel original) {
        final PathElement path = original.getPath(name);
        if(path == null) {
            return null;
        }
        return new HostPathAdd(new PathElementUpdate(name, path.getPath(), path.getRelativeTo()));
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerPathRemove(name);
    }

}
