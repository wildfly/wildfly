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
import java.util.NoSuchElementException;

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
public class PatchArtifact extends ArtifactWithCollectionState<PatchingHistoryRoot.State, PatchArtifact.State, PatchArtifact.CollectionState> {

    private static final PatchArtifact INSTANCE = new PatchArtifact();

    public static PatchArtifact getInstance() {
        return INSTANCE;
    }

    private final Artifact<CollectionState, PatchHistoryDir.State> historyDirArtifact;

    private PatchArtifact() {
        historyDirArtifact = addArtifact(PatchHistoryDir.getInstance());
    }

    public class CollectionState extends ArtifactCollectionState<State> {

        private final State head;
        private State current;

        CollectionState(Context ctx, PatchingHistoryRoot.State parent, TargetInfo identity) {

            String patchId = identity.getCumulativePatchID();
            Patch.PatchType type = Patch.PatchType.CUMULATIVE;
            if(!identity.getPatchIDs().isEmpty()) {
                type = Patch.PatchType.ONE_OFF;
                patchId = identity.getPatchIDs().get(0);
            } else if(patchId.equals(Constants.BASE)){
                head = null;
                return;
            }

            head = new State(this, patchId, type);
            resetIndex();
        }

        @Override
        public State getState() {
            if(current == null) {
                if(head == null) {
                    throw new NoSuchElementException();
                }
                return head;
            }
            return current;
        }

        @Override
        public void resetIndex() {
            current = null;
        }

        @Override
        public boolean hasNext(Context ctx) {
            if(current == null) {
                return head != null;
            }
            return current.hasPrevious(ctx);
        }

        @Override
        public State next(Context ctx) {
            if(!hasNext(ctx)) {
                throw new NoSuchElementException();
            }
            if(current == null) {
                current = head;
            } else {
                current = current.toPrevious(ctx);
            }
            return current;
        }
    }

    public class State implements Artifact.State {

        private final CollectionState col;
        private final String patchId;
        private final PatchType type;

        PatchHistoryDir.State historyDir;

        protected State previous;

        State(CollectionState col, String patchId, PatchType type) {
            this.col = col;
            this.patchId = patchId;
            this.type = type;
        }

        State(CollectionState col, RollbackPatch patch, Context ctx) throws IOException {
            this.col = col;
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

        public PatchHistoryDir.State getHistoryDir(Context ctx) {
            return historyDir == null ? historyDirArtifact.getState(col, ctx) : historyDir;
        }

        public boolean hasPrevious(Context ctx) {
            getHistoryDir(ctx);
            if(!historyDir.getRollbackXml(ctx).getFile().exists()) {
                return false;
            }
            final RollbackPatch patch = (RollbackPatch) historyDir.getRollbackXml(ctx).getPatch(ctx);
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
            return col.hasNext(ctx) ? col.next(ctx) : null;
        }

        private State toPrevious(Context ctx) {
            if(previous == null) {
                if(!hasPrevious(ctx)) {
                    return null;
                }
                try {
                    previous = new State(col, (RollbackPatch)historyDir.getRollbackXml(ctx).getPatch(ctx), ctx);
                } catch (IOException e) {
                    ctx.getErrorHandler().error("Failed to load previous patch", e);
                    return null;
                }
                //validateForState(ctx, previous);
            }
            return previous;
        }
    }

    @Override
    protected CollectionState getInitialState(PatchingHistoryRoot.State parent, Context ctx) {
        if (parent.patches == null) {
            final Identity identity = ctx.getInstallationManager().getIdentity();
            TargetInfo identityInfo;
            try {
                identityInfo = identity.loadTargetInfo();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            parent.patches = new CollectionState(ctx, parent, identityInfo);
        }
        return parent.patches;
    }

}
