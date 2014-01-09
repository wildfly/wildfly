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

import org.jboss.as.patching.installation.InstalledIdentity;

/**
 * Artifact representing the patch history. This validates the patch.xml and rollback.xml. Misc files and overlays are
 * validated as part of the patch.xml, to determine whether they have to exist or not.
 *
 * @author Alexey Loubyansky
 * @author Emanuel Muckenhuber
 */
class PatchingHistoryDirArtifact extends AbstractArtifact<PatchingArtifacts.PatchID, PatchingFileArtifact.DirectoryArtifactState>
        implements PatchingFileArtifact<PatchingArtifacts.PatchID, PatchingFileArtifact.DirectoryArtifactState> {

    PatchingHistoryDirArtifact() {
        super(PatchingArtifacts.PATCH_XML, PatchingArtifacts.ROLLBACK_XML);
    }

    @Override
    public boolean process(PatchingArtifacts.PatchID parent, PatchingArtifactProcessor processor) {
        final InstalledIdentity identity = processor.getInstalledIdentity();
        final File history = identity.getInstalledImage().getPatchHistoryDir(parent.getPatchID());
        final PatchingFileArtifact.DirectoryArtifactState state = new PatchingFileArtifact.DirectoryArtifactState(history, this);
        return processor.process(this, state);
    }

}

