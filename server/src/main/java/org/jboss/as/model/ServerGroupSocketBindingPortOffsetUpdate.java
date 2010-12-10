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
 * Update to add a {@link JvmElement} to a {@link ServerGroupElement}.
 *
 * @author Brian Stansberry
 */
public class ServerGroupSocketBindingPortOffsetUpdate extends AbstractModelUpdate<ServerGroupElement, Void> {

    private static final long serialVersionUID = -5766717739615737224L;

    private final int offset;

    public ServerGroupSocketBindingPortOffsetUpdate(final int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset " + offset + " is less than zero");
        this.offset = offset;
    }

    @Override
    public ServerGroupSocketBindingPortOffsetUpdate getCompensatingUpdate(ServerGroupElement original) {
        return new ServerGroupSocketBindingPortOffsetUpdate(original.getSocketBindingPortOffset());
    }

    @Override
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        // Socket binding changes do not affect running servers; they are picked up by
        // HostController when it launches servers
        return null;
    }

    @Override
    protected void applyUpdate(ServerGroupElement element) throws UpdateFailedException {
        element.setSocketBindingPortOffset(offset);
    }

    @Override
    public Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }

}
