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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

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

    private final Artifact<State, PatchElementProviderArtifact.State> layerArtifact;

    private PatchElementArtifact() {
        layerArtifact = addArtifact(PatchElementProviderArtifact.getInstance());
    }

    public class ElementState implements Artifact.State {

        private final State col;
        private final PatchElement metadata;
        PatchElementProviderArtifact.State layer;

        ElementState(State col, PatchElement metadata) {
            this.col = col;
            this.metadata = metadata;
        }

        PatchElement getMetadata() {
            return metadata;
        }

        public PatchElementProviderArtifact.State getLayer(Context ctx) {
            return layer == null ? layerArtifact.getState(col, ctx) : layer;
        }

        @Override
        public void validate(Context ctx) {
            //System.out.println("ElementState validate " + metadata.getId());
        }
    }

    public class State extends ArtifactCollectionState<ElementState> {

        private List<ElementState> list = Collections.emptyList();
        private int i = -1;

        State(List<PatchElement> elements) {
            for(PatchElement e : elements) {
                add(new ElementState(this, e));
            }
        }

        protected void add(ElementState item) {
            switch(list.size()) {
                case 0:
                    list = Collections.singletonList(item);
                    break;
                case 1:
                    final List<ElementState> tmp = list;
                    list = new ArrayList<ElementState>();
                    list.add(tmp.get(0));
                default:
                    list.add(item);
            }
        }

        @Override
        protected ElementState getState() {
            int size = list.size();
            if(size == 0 || i >= size) {
                return null;
            }
            if(i < 0) {
                return list.get(0);
            }
            return list.get(i);
        }

        @Override
        public void resetIndex() {
            this.i = -1;
        }

        @Override
        public boolean hasNext(Context ctx) {
            return i < list.size() - 1;
        }

        @Override
        public ElementState next(Context ctx) {
            if(!hasNext(ctx)) {
                throw new NoSuchElementException();
            }
            return list.get(++i);
        }
    }

    @Override
    protected State getInitialState(PatchXmlArtifact.State parent, Context ctx) {
        if(parent.patchElements == null) {
            parent.patchElements = new PatchElementArtifact.State(parent.getPatch(ctx).getElements());
        }
        return parent.patchElements;
    }
}
