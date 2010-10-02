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
public final class ServerSubsystemRemove extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = 4805171132512065204L;

    private final String subsystemUri;

    /**
     * Construct a new instance.
     *
     * @param uri the subsystem URI
     */
    public ServerSubsystemRemove(final String uri) {
        super(true);
        subsystemUri = uri;
    }

    public String getSubsystemUri() {
        return subsystemUri;
    }

    protected void applyUpdate(final ServerModel element) throws UpdateFailedException {

    }

    public <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        // no runtime operation
    }

    public ServerSubsystemAdd getCompensatingUpdate(final ServerModel original) {
        return null;
    }
}
