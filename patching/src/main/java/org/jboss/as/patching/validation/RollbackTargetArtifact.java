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

import java.io.IOException;
import java.util.List;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.RollbackPatch;

/**
 * Artifact validating that the overall patches are consistent with the rollback information.
 *
 * @author Emanuel Muckenhuber
 */
class RollbackTargetArtifact extends AbstractArtifact<PatchingXmlArtifact.XmlArtifactState<RollbackPatch>, RollbackTargetArtifact.State> {

    @Override
    public boolean process(PatchingXmlArtifact.XmlArtifactState<RollbackPatch> parent, PatchingArtifactProcessor processor) {
        final RollbackPatch patch = parent.getPatch();
        final InstalledIdentity identity = patch.getIdentityState();
        final PatchingArtifacts.PatchID patchID = processor.getParentArtifact(PatchingArtifacts.HISTORY_RECORD);
        final State state = new State(identity, patchID);
        if (identity == null) {
            processor.getValidationContext().addMissing(PatchingArtifacts.ROLLBACK_TARGET, state);
            return false;
        } else {
            return processor.process(this, state);
        }
    }

    static class State implements PatchingArtifact.ArtifactState {

        private final InstalledIdentity rollbackIdentity;
        private final PatchingArtifacts.PatchID reference;

        State(InstalledIdentity rollbackIdentity, PatchingArtifacts.PatchID reference) {
            this.rollbackIdentity = rollbackIdentity;
            this.reference = reference;
        }

        @Override
        public boolean isValid(PatchingArtifactValidationContext context) {
            try {
                // Check the target state we are rolling back to
                final PatchableTarget.TargetInfo target = rollbackIdentity.getIdentity().loadTargetInfo();
                final List<String> patches = target.getPatchIDs();
                final String rollbackTo;
                if (patches.isEmpty()) {
                    rollbackTo = target.getCumulativePatchID();
                } else {
                    rollbackTo = patches.get(0);
                }
                final String ref = reference.getNextPatchID();
                if (rollbackTo.equals(ref)) {
                    return true;
                } else if (ref == null && Constants.BASE.equals(rollbackTo)) {
                    return true;
                } else {
                    context.addInconsistent(PatchingArtifacts.ROLLBACK_TARGET, this);
                }
            } catch (IOException e) {
                context.addError(PatchingArtifacts.ROLLBACK_TARGET, this);
            }
            return false;
        }

        @Override
        public String toString() {
            return reference.getPatchID() != null ? reference.getPatchID() : Constants.BASE;
        }
    }

}
