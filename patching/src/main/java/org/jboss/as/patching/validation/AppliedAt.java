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
public class AppliedAt extends AbstractArtifact<PatchHistoryDir.State, AppliedAt.State> {

    public static final AppliedAt INSTANCE = new AppliedAt();

    public static class State implements Artifact.State {

        private final File file;

        State(File file) {
            this.file = file;
        }

        @Override
        public void validate(Context ctx) {
            // TODO exists, can be parsed
            file.exists();
        }
    }

    @Override
    protected State getInitialState(PatchHistoryDir.State historyDir, Context ctx) {
        State state = historyDir.getAppliedAt();
        if(state == null) {
            state = new State(new File(historyDir.getDirectory(), "timestamp"));
            historyDir.setAppliedAt(state);
        }
        return state;
    }
}
