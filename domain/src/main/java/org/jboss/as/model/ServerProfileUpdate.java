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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerProfileUpdate<E extends AbstractSubsystemElement<E>, R> extends AbstractServerModelUpdate<R> {

    private static final long serialVersionUID = -9127280126443066490L;

    private final AbstractSubsystemUpdate<E, R> subsystemUpdate;

    public ServerProfileUpdate(final AbstractSubsystemUpdate<E, R> subsystemUpdate) {
        this.subsystemUpdate = subsystemUpdate;
    }

    public static <E extends AbstractSubsystemElement<E>, R> ServerProfileUpdate<E, R> create(AbstractSubsystemUpdate<E, R> subsystemUpdate) {
        return new ServerProfileUpdate<E, R>(subsystemUpdate);
    }

    protected void applyUpdate(final ServerModel element) throws UpdateFailedException {
        final String namespaceUri = subsystemUpdate.getSubsystemNamespaceUri();
        final E subsystemElement = subsystemUpdate.getModelElementType().cast(element.getSubsystem(namespaceUri));
        if (subsystemElement == null) {
            throw new IllegalArgumentException("No such subsystem '" + namespaceUri + "' declared on server instance");
        }
        subsystemUpdate.applyUpdate(subsystemElement);
    }

    public <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super R, P> handler, final P param) {
        subsystemUpdate.applyUpdate(updateContext, handler, param);
    }

    public ServerProfileUpdate<E, ?> getCompensatingUpdate(final ServerModel original) {
        final String namespaceUri = subsystemUpdate.getSubsystemNamespaceUri();
        final E element = subsystemUpdate.getModelElementType().cast(original.getSubsystem(namespaceUri));
        if (element == null) {
            throw new IllegalArgumentException("No such subsystem '" + namespaceUri + "' declared on server instance");
        }
        return createUpdate(subsystemUpdate.getCompensatingUpdate(element));
    }

    private static <E extends AbstractSubsystemElement<E>, R> ServerProfileUpdate<E, R> createUpdate(final AbstractSubsystemUpdate<E, R> update) {
        return new ServerProfileUpdate<E,R>(update);
    }
}
