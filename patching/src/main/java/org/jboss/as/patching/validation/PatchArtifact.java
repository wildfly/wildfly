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

import java.io.IOException;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.PatchableTarget.TargetInfo;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.RollbackPatch;


/**
 * @author Alexey Loubyansky
 *
 */
public class PatchArtifact extends AbstractArtifact<PatchingHistory.State, PatchArtifact.State> {

    public static final PatchArtifact INSTANCE = new PatchArtifact();

    private PatchArtifact() {
        addArtifact(PatchHistoryDir.INSTANCE);
    }

    public class State implements Artifact.State {

        private final String patchId;
        private PatchType type;

        private PatchHistoryDir.State historyDir;

        protected State previous;

        State(String patchId, PatchType type) {
            this.patchId = patchId;
            this.type = type;
        }

        State(RollbackPatch patch, Context ctx) throws IOException {
            final TargetInfo info = patch.getIdentityState().getIdentity().loadTargetInfo();
            if(info.getPatchIDs().isEmpty()) {
                patchId = info.getCumulativePatchID();
                type = PatchType.CUMULATIVE;
            } else {
                patchId = info.getPatchIDs().get(0);
                type = PatchType.ONE_OFF;
            }
        }

        public String getPatchId() {
            return patchId;
        }

        public PatchType getType() {
            return type;
        }

        @Override
        public void validate(Context ctx) {
            // TODO Auto-generated method stub
        }

        public PatchHistoryDir.State getHistoryDir() {
            return historyDir;
        }

        public void setHistoryDir(PatchHistoryDir.State historyDir) {
            this.historyDir = historyDir;
        }

        public boolean hasPrevious(Context ctx) {
            if(!historyDir.getRollbackXml().getFile().exists()) {
                return false;
            }
            final RollbackPatch patch = (RollbackPatch) historyDir.getRollbackXml().getPatch();
            TargetInfo targetInfo;
            try {
                targetInfo = patch.getIdentityState().getIdentity().loadTargetInfo();
            } catch (IOException e) {
                ctx.getErrorHandler().error("Failed to load identity info for patch " + patch.getPatchId(), e);
                return false;
            }
            return !Constants.BASE.equals(targetInfo.getCumulativePatchID()) || !targetInfo.getPatchIDs().isEmpty();
        }

        public State getPrevious(Context ctx) {
            if(previous == null) {
                if(!hasPrevious(ctx)) {
                    return null;
                }
                try {
                    previous = new State((RollbackPatch)historyDir.getRollbackXml().getPatch(), ctx);
                } catch (IOException e) {
                    ctx.getErrorHandler().error("Failed to load previous patch", e);
                    return null;
                }
                validateForState(ctx, previous);
            }
            return previous;
        }
    }

    @Override
    protected State getInitialState(PatchingHistory.State parent, Context ctx) {
        State lastApplied = parent.getLastAppliedPatch();
        if(lastApplied != null) {
            return lastApplied;
        }
        final Identity identity = ctx.getInstallationManager().getIdentity();
        TargetInfo identityInfo;
        try {
            identityInfo = identity.loadTargetInfo();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        String patchId = identityInfo.getCumulativePatchID();
        Patch.PatchType type = Patch.PatchType.CUMULATIVE;
        if(!identityInfo.getPatchIDs().isEmpty()) {
            type = Patch.PatchType.ONE_OFF;
            patchId = identityInfo.getPatchIDs().get(0);
        } else if(patchId.equals(Constants.BASE)){
            return null;
        }
        lastApplied = new State(patchId, type);
        parent.setLastAppliedPatch(lastApplied);
        return lastApplied;
    }

}
