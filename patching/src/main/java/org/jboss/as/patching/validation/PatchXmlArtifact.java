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
public class PatchXmlArtifact extends AbstractArtifact<PatchHistoryDir.State, PatchXmlArtifact.State> {

    public static final PatchXmlArtifact INSTANCE = new PatchXmlArtifact();

    public static final PatchXmlArtifact getInstance() {
        return INSTANCE;
    }

    private final Artifact<State, PatchElementArtifact.State> elementsArtifact;

    private PatchXmlArtifact() {
        elementsArtifact = addArtifact(PatchElementArtifact.getInstance());
    }

    public class State extends XmlFileState {

        PatchElementArtifact.State patchElements;

        State(File file) {
            super(file);
        }

        public PatchElementArtifact.State getPatchElements(Context ctx) {
            return patchElements == null ? elementsArtifact.getState(this, ctx) : patchElements;
        }
    }

    @Override
    protected State getInitialState(PatchHistoryDir.State historyDir, Context ctx) {
        if(historyDir.patchXml == null) {
            historyDir.patchXml = new State(new File(historyDir.getDirectory(), "patch.xml"));
        }
        return historyDir.patchXml;
    }
}
