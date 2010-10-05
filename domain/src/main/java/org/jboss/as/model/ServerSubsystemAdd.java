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
public final class ServerSubsystemAdd extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = 4755195359412875338L;

    private final AbstractSubsystemAdd<?> subsystemAdd;

    public ServerSubsystemAdd(final AbstractSubsystemAdd<?> subsystemAdd) {
        this.subsystemAdd = subsystemAdd;
    }

    public AbstractSubsystemAdd<?> getSubsystemAdd() {
        return subsystemAdd;
    }

    public String getNamespaceUri() {
        return subsystemAdd.getNamespaceUri();
    }

    protected void applyUpdate(final ServerModel element) throws UpdateFailedException {
        subsystemAdd.applyUpdate(element.getProfile());
    }

    public <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {

    }

    public ServerSubsystemRemove getCompensatingUpdate(final ServerModel original) {
        return new ServerSubsystemRemove(subsystemAdd.getCompensatingUpdate(original.getProfile()));
    }
}
