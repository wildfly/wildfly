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

import java.io.File;

import org.jboss.as.patching.DirectoryStructure;

/**
 * Artifact representing either a module or bundle overlay directory.
 *
 * @author Emanuel Muckenhuber
 */
class PatchingOverlayDirArtifact extends AbstractArtifact<PatchableTargetsArtifact.PatchableTargetState, PatchingFileArtifact.DirectoryArtifactState>
        implements PatchingFileArtifact<PatchableTargetsArtifact.PatchableTargetState, PatchingFileArtifact.DirectoryArtifactState> {

    private final boolean bundles;

    protected PatchingOverlayDirArtifact(boolean bundles, PatchingArtifact<PatchingFileArtifact.DirectoryArtifactState, ? extends ArtifactState>... artifacts) {
        super(artifacts);
        this.bundles = bundles;
    }

    @Override
    public boolean process(PatchableTargetsArtifact.PatchableTargetState parent, PatchingArtifactProcessor processor) {
        if (bundles && !parent.isCheckBundles()) {
            return true;
        } else if (!parent.isCheckModules()) {
            return true;
        }
        final String patchID = parent.getPatchID();
        final DirectoryStructure structure = parent.getStructure();
        final File overlay = bundles ? structure.getBundlesPatchDirectory(patchID) : structure.getModulePatchDirectory(patchID);
        final PatchingFileArtifact.DirectoryArtifactState state = new PatchingFileArtifact.DirectoryArtifactState(overlay, this);
        return processor.process(this, state);
    }

}
