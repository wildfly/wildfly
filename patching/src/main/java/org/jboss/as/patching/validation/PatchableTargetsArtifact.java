/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchElementProvider;

/**
 * Artifact validating the state of a {@code PatchableTarget}.
 *
 * @author Emanuel Muckenhuber
 */
class PatchableTargetsArtifact extends AbstractArtifact<PatchingXmlArtifact.XmlArtifactState<Patch>, PatchableTargetsArtifact.PatchableTargetState> {

    PatchableTargetsArtifact(PatchingArtifact<PatchableTargetState, ? extends ArtifactState>... artifacts) {
        super(artifacts);
    }

    @Override
    public boolean process(PatchingXmlArtifact.XmlArtifactState<Patch> parent, PatchingArtifactProcessor processor) {
        final InstalledIdentity identity = processor.getInstalledIdentity();
        final Patch patch = parent.getPatch();
        if (Constants.BASE.equals(patch.getPatchId())) {
            return true;
        }
        final List<PatchElement> elements = patch.getElements();
        boolean valid = true;
        if (elements != null && !elements.isEmpty()) {
            for (final PatchElement element : elements) {
                final String patchID = element.getId();
                final PatchElementProvider provider = element.getProvider();
                final String layerName = provider.getName();
                final PatchableTarget target = provider.isAddOn() ? identity.getAddOn(layerName) : identity.getLayer(layerName);
                boolean modules = false;
                boolean bundles = false;
                for (final ContentModification modification : element.getModifications()) {
                    if (modules && bundles) {
                        break;
                    }
                    if (modification.getItem().getContentType() == ContentType.BUNDLE) {
                        bundles = true;
                    } else if (modification.getItem().getContentType() == ContentType.MODULE) {
                        modules = true;
                    }
                }
                final PatchableTargetState state = new PatchableTargetState(patchID, layerName, target, bundles, modules);
                if (!processor.process(PatchingArtifacts.LAYER, state)) {
                    valid = false;
                }
            }
        }

        // In case there are misc content modifications also validate that misc backup exists
        for (final ContentModification modification : patch.getModifications()) {
            if (modification.getType() != ModificationType.ADD && modification.getItem().getContentType() == ContentType.MISC) {
                final PatchingHistoryDirArtifact.DirectoryArtifactState history = processor.getParentArtifact(PatchingArtifacts.HISTORY_DIR);
                PatchingArtifacts.MISC_BACKUP.process(history, processor);
                break;
            }
        }
        return valid;
    }

    static class PatchableTargetState implements PatchingArtifact.ArtifactState {

        private final String patchID;
        private final String layerName;
        private final PatchableTarget target;
        private final boolean checkBundles;
        private final boolean checkModules;

        PatchableTargetState(String patchID, String layerName, PatchableTarget target, boolean checkBundles, boolean checkModules) {
            this.patchID = patchID;
            this.layerName = layerName;
            this.target = target;
            this.checkBundles = checkBundles;
            this.checkModules = checkModules;
        }

        public String getPatchID() {
            return patchID;
        }

        public DirectoryStructure getStructure() {
            return target.getDirectoryStructure();
        }

        public boolean isCheckBundles() {
            return checkBundles;
        }

        public boolean isCheckModules() {
            return checkModules;
        }

        @Override
        public boolean isValid(PatchingArtifactValidationContext context) {
            if (target == null) {
                context.addMissing(PatchingArtifacts.PATCH_CONTENTS, this);
            }
            return true;
        }

        @Override
        public String toString() {
            return layerName;
        }
    }

}
