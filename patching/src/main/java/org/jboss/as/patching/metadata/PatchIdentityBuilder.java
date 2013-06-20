/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.metadata;

import org.jboss.as.patching.metadata.impl.IdentityImpl;
import org.jboss.as.patching.metadata.impl.IncompatibleWithCallback;
import org.jboss.as.patching.metadata.impl.RequiresCallback;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchIdentityBuilder implements RequiresCallback, IncompatibleWithCallback {

    private final PatchBuilder parent;
    private final IdentityImpl identity;

    public PatchIdentityBuilder(final String name, final String version, final Patch.PatchType patchType, final PatchBuilder parent) {
        this.identity = new IdentityImpl(name, version);
        this.identity.setPatchType(patchType);
        this.parent = parent;
    }

    IdentityImpl getIdentity() {
        return identity;
    }

    @Override
    public PatchIdentityBuilder incompatibleWith(String patchID) {
        identity.incompatibleWith(patchID);
        return this;
    }

    @Override
    public PatchIdentityBuilder require(String id) {
        identity.require(id);
        return this;
    }

    public PatchBuilder getParent() {
        return parent;
    }

}
