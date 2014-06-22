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

import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.metadata.impl.IncompatibleWithCallback;
import org.jboss.as.patching.metadata.impl.PatchElementImpl;
import org.jboss.as.patching.metadata.impl.PatchElementProviderImpl;
import org.jboss.as.patching.metadata.impl.RequiresCallback;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchElementBuilder extends ModificationBuilderTarget<PatchElementBuilder>
        implements PatchBuilder.PatchElementHolder, RequiresCallback, IncompatibleWithCallback {

    private final String patchId;
    private final PatchElementImpl element;
    private PatchElementProviderImpl provider;
    private final PatchBuilder parent;

    protected PatchElementBuilder(final String patchId, final String layerName, final boolean addOn, final PatchBuilder parent) {
        this.patchId = patchId;
        this.provider = new PatchElementProviderImpl(layerName, addOn);
        this.element = new PatchElementImpl(patchId);
        element.setProvider(provider);
        this.parent = parent;
    }

    PatchElementProviderImpl getProvider() {
        return provider;
    }

    public PatchElementBuilder setDescription(String description) {
        element.setDescription(description);
        return this;
    }

    @Override
    protected PatchElementBuilder internalAddModification(ContentModification modification) {
        element.addContentModification(modification);
        return returnThis();
    }

    @Override
    public IncompatibleWithCallback incompatibleWith(String patchID) {
        provider.incompatibleWith(patchID);
        return returnThis();
    }

    @Override
    public PatchElementBuilder require(String id) {
        provider.require(id);
        return returnThis();
    }

    public PatchElementBuilder upgrade() {
        provider.upgrade();
        return returnThis();
    }

    public PatchElementBuilder oneOffPatch() {
        provider.oneOffPatch();
        return returnThis();
    }

    public PatchElement createElement(Patch.PatchType patchType) {
        assert patchId != null;
        assert provider != null;
        if (patchType != getProvider().getPatchType()) {
            throw PatchLogger.ROOT_LOGGER.patchTypesDontMatch();
        }
        return element;
    }

    public PatchBuilder getParent() {
        return parent;
    }

    @Override
    protected PatchElementBuilder returnThis() {
        return this;
    }

}
