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
public class PatchHistoryDir extends AbstractArtifact<PatchArtifact.State,PatchHistoryDir.State> {

    public static final PatchHistoryDir INSTANCE = new PatchHistoryDir();

    private PatchHistoryDir() {
        addArtifact(PatchXml.INSTANCE);
        addArtifact(RollbackXml.INSTANCE);
    }

    public static class State implements Artifact.State {

        private final File dir;

        State(File dir) {
            this.dir = dir;
        }

        private PatchXml.State patchXml;
        private RollbackXml.State rollbackXml;
        private AppliedAt.State appliedAt;
        // TODO configuration dir

        public File getDirectory() {
            return dir;
        }

        public RollbackXml.State getRollbackXml() {
            return rollbackXml;
        }

        public void setRollbackXml(RollbackXml.State rollbackXml) {
            this.rollbackXml = rollbackXml;
        }

        public PatchXml.State getPatchXml() {
            return patchXml;
        }

        public void setPatchXml(PatchXml.State patchXml) {
            this.patchXml = patchXml;
        }

        public AppliedAt.State getAppliedAt() {
            return appliedAt;
        }

        public void setAppliedAt(AppliedAt.State appliedAt) {
            this.appliedAt = appliedAt;
        }

        @Override
        public void validate(Context ctx) {
            // TODO Auto-generated method stub

        }

    }

    @Override
    protected State getInitialState(PatchArtifact.State patch, Context ctx) {
        State historyDir = patch.getHistoryDir();
        if(historyDir != null) {
            return historyDir;
        }
        final File dir = ctx.getInstallationManager().getInstalledImage().getPatchHistoryDir(patch.getPatchId());
        historyDir = new State(dir);
        patch.setHistoryDir(historyDir);
        return historyDir;
    }
}
