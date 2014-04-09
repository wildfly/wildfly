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

import java.io.File;

import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.PatchElementProvider;


/**
 * @author Alexey Loubyansky
 *
 */
public class PatchElementProviderArtifact extends AbstractArtifact<PatchElementArtifact.State, PatchElementProviderArtifact.State> {

    private static final PatchElementProviderArtifact INSTANCE = new PatchElementProviderArtifact();

    public static PatchElementProviderArtifact getInstance() {
        return INSTANCE;
    }

    public static class State implements Artifact.State {

        private final PatchableTarget layer;
        private final String patchId;

        State(Layer layer, String patchId) {
            if(layer == null) {
                throw new IllegalArgumentException("layer is null");
            }
            if(patchId == null) {
                throw new IllegalArgumentException("patch element id is null");
            }
            this.layer = layer;
            this.patchId = patchId;
        }

        @Override
        public void validate(Context ctx) {
            //System.out.println("Layer validate " + layer.getName());
        }

        public File getBundlesDir() {
            final File dir  = layer.getDirectoryStructure().getBundlesPatchDirectory(patchId);
            return dir == null ? null : dir.exists() ? dir : null;
        }

        public File getModulesDir() {
            final File dir = layer.getDirectoryStructure().getModulePatchDirectory(patchId);
            return dir == null ? null : dir.exists() ? dir : null;
        }
    }

    @Override
    protected State getInitialState(PatchElementArtifact.State parent, Context ctx) {
        State state = parent.getLayer();
        if(state != null) {
            return state;
        }
        final PatchElementProvider metadata = parent.getMetadata().getProvider();
        final Layer layer = ctx.getInstallationManager().getLayer(metadata.getName());
        if(layer == null) {
            ctx.getErrorHandler().error("Layer not found: " + metadata.getName()); // no i18n needed
            return null;
        }

        state = new State(layer, parent.getMetadata().getId());
        parent.setLayer(state);
        return state;
    }
}
