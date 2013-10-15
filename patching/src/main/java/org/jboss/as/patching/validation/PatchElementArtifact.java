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

package org.jboss.as.patching.validation;

import java.util.List;

import org.jboss.as.patching.metadata.PatchElement;

/**
 * @author Alexey Loubyansky
 *
 */
public class PatchElementArtifact extends ArtifactWithCollectionState<PatchXmlArtifact.State, PatchElementArtifact.ElementState, PatchElementArtifact.State> {

    private static final PatchElementArtifact INSTANCE = new PatchElementArtifact();

    public static PatchElementArtifact getInstance() {
        return INSTANCE;
    }

    private PatchElementArtifact() {
        addArtifact(PatchElementProviderArtifact.getInstance());
    }

    public class ElementState implements Artifact.State {

        private PatchElement metadata;

        private PatchElementProviderArtifact.State layer;

        void init(PatchElement metadata) {
            this.metadata = metadata;
        }

        PatchElement getMetadata() {
            return metadata;
        }

        public PatchElementProviderArtifact.State getLayer() {
            return layer;
        }

        void setLayer(PatchElementProviderArtifact.State layer) {
            this.layer = layer;
        }

        @Override
        public void validate(Context ctx) {
            //System.out.println("ElementState validate " + metadata.getId());
        }
    }

    public class State extends ArtifactCollectionState<ElementState> {

        State() {}

        State(List<PatchElement> elements) {
            for(PatchElement e : elements) {
                final ElementState s = newItem();
                s.init(e);
            }
        }

        @Override
        protected ElementState createItem() {
            return new ElementState();
        }

        public PatchElementProviderArtifact.State getLayer() {
            final ElementState state = getState();
            if(state == null) {
                return null;
            }
            return state.getLayer();
        }

        void setLayer(PatchElementProviderArtifact.State layer) {
            final ElementState state = getState();
            if(state == null) {
                throw new IllegalStateException("patch element doesn't exist");
            }
            state.setLayer(layer);
        }

        public PatchElement getMetadata() {
            final ElementState state = getState();
            if(state == null) {
                return null;
            }
            return state.getMetadata();
        }
    }

    @Override
    protected State getInitialState(PatchXmlArtifact.State parent, Context ctx) {
        State elements = parent.getPatchElements();
        if(elements != null) {
            return elements;
        }
        elements = new PatchElementArtifact.State(parent.getPatch().getElements());
        parent.setPatchElements(elements);
        return elements;
    }
}
