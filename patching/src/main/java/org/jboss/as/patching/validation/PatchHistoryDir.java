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


/**
 * @author Alexey Loubyansky
 *
 */
public class PatchHistoryDir extends AbstractArtifact<PatchArtifact.CollectionState,PatchHistoryDir.State> {

    private static final PatchHistoryDir INSTANCE = new PatchHistoryDir();

    public static PatchHistoryDir getInstance() {
        return INSTANCE;
    }

    private final Artifact<State, PatchXmlArtifact.State> patchXmlArtifact;
    private final Artifact<State, RollbackXmlArtifact.State> rollbackXmlArtifact;

    private PatchHistoryDir() {
        patchXmlArtifact = addArtifact(PatchXmlArtifact.getInstance());
        rollbackXmlArtifact = addArtifact(RollbackXmlArtifact.getInstance());
    }

    public class State implements Artifact.State {

        private final File dir;

        State(File dir) {
            this.dir = dir;
        }

        protected PatchXmlArtifact.State patchXml;
        protected RollbackXmlArtifact.State rollbackXml;
        private AppliedAtArtifact.State appliedAt;
        // TODO configuration dir

        public File getDirectory() {
            return dir;
        }

        public void setRollbackXml(RollbackXmlArtifact.State rollbackXml) {
            this.rollbackXml = rollbackXml;
        }

        public void setPatchXml(PatchXmlArtifact.State patchXml) {
            this.patchXml = patchXml;
        }

        public RollbackXmlArtifact.State getRollbackXml(Context ctx) {
            return rollbackXml == null ? rollbackXmlArtifact.getState(this, ctx) : rollbackXml;
        }

        public PatchXmlArtifact.State getPatchXml(Context ctx) {
            return patchXml == null ? patchXmlArtifact.getState(this, ctx) : patchXml;
        }

        public AppliedAtArtifact.State getAppliedAt() {
            return appliedAt;
        }

        public void setAppliedAt(AppliedAtArtifact.State appliedAt) {
            this.appliedAt = appliedAt;
        }

        @Override
        public void validate(Context ctx) {
            // TODO Auto-generated method stub

        }

    }

    @Override
    protected State getInitialState(PatchArtifact.CollectionState patchCollection, Context ctx) {
        PatchArtifact.State patch = patchCollection.getState();
        if(patch == null) {
            return null;
        }
        if(patch.historyDir == null) {
            final File dir = ctx.getInstallationManager().getInstalledImage().getPatchHistoryDir(patch.getPatchId());
            patch.historyDir = new State(dir);
        }
        return patch.historyDir;
    }
}
