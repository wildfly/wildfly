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

import java.util.Collection;

/**
 * A generic validatable artifact as part of the patching state. All artifacts have a unique identity, but can have
 * a different {@code ArtifactState} depending on the context.
 *
 * @author Alexey Loubyansky
 * @author Emanuel Muckenhuber
 */
public interface PatchingArtifact<P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> {

    /**
     * Process the artifact and push it to the {@code PatchingArtifactProcessor} for further analysis.
     *
     * @param parent    the parent state
     * @param processor the processor
     * @return whether the current artifact and it's children are valid
     */
    boolean process(final P parent, final PatchingArtifactProcessor processor);

    /**
     * Get the associated child artifacts.
     *
     * @return the child artifacts
     */
    Collection<PatchingArtifact<S, ? extends ArtifactState>> getArtifacts();

    interface ArtifactState {

        /**
         * Check if a state is consistent.
         *
         * @param context the validation context
         * @return whether the artifact is valid
         */
        boolean isValid(PatchingArtifactValidationContext context);

    }

}
