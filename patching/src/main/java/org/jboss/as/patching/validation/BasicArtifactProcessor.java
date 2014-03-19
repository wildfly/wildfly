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

import org.jboss.as.patching.installation.InstalledIdentity;

/**
 * @author Emanuel Muckenhuber
 */
class BasicArtifactProcessor implements PatchingArtifactProcessor {

    private final PatchHistoryValidations.PatchingArtifactStateHandlers handlers;
    private final PatchingArtifactValidationContext context;

    // The current node
    private Node current;

    public BasicArtifactProcessor(final InstalledIdentity installedIdentity, final PatchingValidationErrorHandler errorHandler,
                                  final PatchHistoryValidations.PatchingArtifactStateHandlers handlers) {
        assert installedIdentity != null;
        assert errorHandler != null;
        assert handlers != null;
        this.context = new PatchingArtifactValidationContext() {

            private InstalledIdentity currentIdentity = installedIdentity;

            @Override
            public PatchingValidationErrorHandler getErrorHandler() {
                if (current.context != null) {
                    return current.context;
                }
                return errorHandler;
            }

            @Override
            public InstalledIdentity getOriginalIdentity() {
                return installedIdentity;
            }

            @Override
            public void setCurrentPatchIdentity(InstalledIdentity currentPatchIdentity) {
                this.currentIdentity = currentPatchIdentity;
                if(currentPatchIdentity == null) {
                    throw new IllegalArgumentException();
                }
            }

            @Override
            public InstalledIdentity getCurrentPatchIdentity() {
                return currentIdentity;
            }
        };
        this.handlers = handlers;
    }

    protected <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> boolean processRoot(PatchingArtifact<P, S> artifact, S state, PatchingValidationErrorHandler context) {
        assert current == null;
        current = new Node(null, artifact, state, context);
        try {
            return doProcess(artifact, state);
        } finally {
            current = null;
        }
    }

    @Override
    public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> boolean process(PatchingArtifact<P, S> artifact, S state) {
        final Node old = current;
        current = new Node(old, artifact, state, old.context);
        try {
            return doProcess(artifact, state);
        } finally {
            current = old;
        }
    }

    public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> boolean doProcess(PatchingArtifact<P, S> artifact, S state) {
        final PatchingArtifactStateHandler<S> handler = getHandlerForArtifact(artifact);
        if (!state.isValid(getValidationContext())) {
            return false;
        }
        // Process each child artifact
        boolean valid = true;
        for (final PatchingArtifact<S, ? extends PatchingArtifact.ArtifactState> child : artifact.getArtifacts()) {
            if (!child.process(state, this)) {
                valid = false;
            }
        }
        if (valid && handler != null) {
            handler.handleValidatedState(state);
        }
        return valid;
    }

    /**
     * Get a state handler for a given patching artifact.
     *
     * @param artifact the patching artifact
     * @param <P>
     * @param <S>
     * @return the state handler, {@code null} if there is no handler registered for the given artifact
     */
    <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> PatchingArtifactStateHandler<S> getHandlerForArtifact(PatchingArtifact<P, S> artifact) {
        return handlers.get(artifact);
    }

    @Override
    public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> S getParentArtifact(PatchingArtifact<P, S> artifact) {
        Node node = current;
        while (node != null) {
            if (node.artifact == artifact) {
                return (S) node.state;
            }
            node = node.parent;
        }
        return null;
    }

    @Override
    public PatchingArtifactValidationContext getValidationContext() {
        return context;
    }

    static class Node<P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> {

        private final S state;
        private final Node parent;
        private final PatchingArtifact<P, S> artifact;
        private final PatchingValidationErrorHandler context;

        Node(Node parent, PatchingArtifact<P, S> artifact, S state, PatchingValidationErrorHandler context) {
            this.state = state;
            this.parent = parent;
            this.artifact = artifact;
            this.context = context;
        }
    }

}
