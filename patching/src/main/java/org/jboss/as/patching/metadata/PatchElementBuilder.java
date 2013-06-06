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

import org.jboss.as.patching.metadata.impl.PatchElementImpl;
import org.jboss.as.patching.metadata.impl.PatchElementProviderImpl;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchElementBuilder extends AbstractModificationBuilderTarget<PatchElementBuilder> implements PatchBuilder.PatchElementHolder {

    private final String patchId;
    private final PatchElementImpl element;
    private PatchElementProvider provider;
    protected PatchElementBuilder(final String patchId, final String layerName, final boolean addOn) {
        this.patchId = patchId;
        this.provider = new PatchElementProviderImpl(layerName, "undefined", addOn);
        this.element = new PatchElementImpl(patchId);
        element.setProvider(provider);
    }

    public PatchElementProvider getProvider() {
        return provider;
    }

    @Override
    public PatchElementBuilder addContentModification(ContentModification modification) {
        element.addContentModification(modification);
        return returnThis();
    }

    public PatchElement createElement(Patch.PatchType patchType) {
        assert patchId != null;
        assert provider != null;

        if (element.getPatchType() == null) {
            if (patchType == Patch.PatchType.CUMULATIVE) {
                element.setUpgrade("unknown");
            } else {
                element.setNoUpgrade();
            }
        }
        return element;
    }

    @Override
    protected PatchElementBuilder returnThis() {
        return this;
    }

}
