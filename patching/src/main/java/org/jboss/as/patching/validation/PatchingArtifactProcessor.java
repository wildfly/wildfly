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
 * The artifact processor.
 *
 * @author Emanuel Muckenhuber
 */
interface PatchingArtifactProcessor {

    /**
     * Process an artifact. This validates the current {@code ArtifactState} and processes the associated
     * {@link PatchingArtifact#getArtifacts()} ()}. This returns {@code false} if there should no be no further processing
     * in {@link PatchingArtifact#process(org.jboss.as.patching.validation.PatchingArtifact.ArtifactState, PatchingArtifactProcessor)}.
     *
     * @param artifact the artifact to process
     * @param state    the parent artifact state
     * @param <P>      the parent artifact state type
     * @param <S>      the current artifact state type
     * @return whether the processing should continue or not
     */
    <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> boolean process(final PatchingArtifact<P, S> artifact, S state);

    /**
     * Get the parent artifact in the call stack. All artifacts have a unique identity and can only be present once
     * as part of the processing.
     *
     * @param artifact the artifact type
     * @param <P>      the parent artifact state type
     * @param <S>      the current artifact state type
     * @return the parent artifact
     */
    <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> S getParentArtifact(final PatchingArtifact<P, S> artifact);

    /**
     * Get the validation context.
     *
     * @return the validation context
     */
    PatchingArtifactValidationContext getValidationContext();

    /**
     * Get the installed identity.
     * <p/>
     * NOTE: this is the original identity and cannot be used as reference to the current patched state as part of
     * processing the history
     *
     * @return the installed identity
     */
    InstalledIdentity getInstalledIdentity();

}
