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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Alexey Loubyansky
 */
abstract class AbstractArtifact<P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> implements PatchingArtifact<P, S> {

    private Collection<PatchingArtifact<S, ? extends ArtifactState>> artifacts = Collections.emptyList();

    protected AbstractArtifact(PatchingArtifact<S, ? extends ArtifactState>... artifacts) {
        for (final PatchingArtifact<S, ? extends ArtifactState> artifact : artifacts) {
            addArtifact(artifact);
        }
    }

    @Override
    public Collection<PatchingArtifact<S, ? extends ArtifactState>> getArtifacts() {
        return artifacts;
    }

    protected void addArtifact(PatchingArtifact<S, ? extends ArtifactState> artifact) {
        assert artifact != null;
        switch (artifacts.size()) {
            case 0:
                artifacts = Collections.<PatchingArtifact<S, ? extends ArtifactState>>singletonList(artifact);
                break;
            case 1:
                artifacts = new ArrayList<PatchingArtifact<S, ? extends ArtifactState>>(artifacts);
            default:
                artifacts.add(artifact);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this);
    }

}
