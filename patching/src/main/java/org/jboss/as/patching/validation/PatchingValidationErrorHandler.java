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

/**
 * The validation context.
 *
 * @author Alexey Loubyansky
 * @author Emanuel Muckenhuber
 */
public interface PatchingValidationErrorHandler {

    /**
     * Add an error when trying to load an artifact.
     *
     * @param artifact the artifact
     * @param state    the artifact state
     * @param <P>      the parent type
     * @param <S>      the current type
     */
    <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addError(PatchingArtifact<P, S> artifact, S state);

    /**
     * Add an inconsistent artifact.
     *
     * @param artifact the artifact
     * @param current  the artifact state
     * @param expected the artifact state
     * @param <P>      the parent type
     * @param <S>      the current type
     */
    <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addInconsistent(PatchingArtifact<P, S> artifact, S current);

    /**
     * Add a missing artifact.
     *
     * @param artifact the artifact
     * @param state    the artifact state
     * @param <P>      the parent type
     * @param <S>      the current type
     */
    <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addMissing(PatchingArtifact<P, S> artifact, S state);

    PatchingValidationErrorHandler DEFAULT = new PatchingValidationErrorHandler() {
        @Override
        public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addError(PatchingArtifact<P, S> artifact, S state) {
            throw new RuntimeException("error when processing artifact " + artifact);
        }

        @Override
        public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addInconsistent(PatchingArtifact<P, S> artifact, S current) {
            throw new RuntimeException("inconsistent artifact " + artifact + ": " + current);
        }

        @Override
        public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addMissing(PatchingArtifact<P, S> artifact, S state) {
            throw new RuntimeException("missing artifact " + artifact + ": " + state);
        }
    };

}
