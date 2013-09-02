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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class AbstractArtifact<P extends Artifact.State, S extends Artifact.State> implements Artifact<P,S> {

    protected Artifact<? extends Artifact.State, P> parent;

    protected List<Artifact<S, ? extends Artifact.State>> artifacts = Collections.emptyList();

    public S getState(Context ctx) {
        return getState2(this, ctx);
    }

    @SuppressWarnings("unchecked")
    protected <A extends Artifact.State, B extends Artifact.State, C extends Artifact.State> A getState2(AbstractArtifact<B,C> artifact, Context ctx) {
        Artifact.State state = null;
        if(artifact.parent != null) {
            B parentState = getState2((AbstractArtifact<B,C>)artifact.parent, ctx);
            return (A)artifact.getState((B)parentState, ctx);
        }
        return (A)artifact.getState((B)state, ctx);
    }

    void addArtifact(Artifact<S, ? extends Artifact.State> a) {
        if(a == null) {
            throw new IllegalArgumentException("Artifact is null");
        }
        switch(artifacts.size()) {
            case 0:
                artifacts = Collections.<Artifact<S, ? extends Artifact.State>>singletonList(a);
                break;
            case 1:
                final Artifact<S, ? extends Artifact.State> first = artifacts.get(0);
                artifacts = new ArrayList<Artifact<S, ? extends Artifact.State>>();
                artifacts.add(first);
            default:
                artifacts.add(a);
        }
    }
    /* (non-Javadoc)
     * @see org.jboss.as.patching.validation.Artifact#getArtifacts()
     */
    @Override
    public Collection<Artifact<S, ? extends Artifact.State>> getArtifacts() {
        return artifacts;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.validation.Artifact#validate()
     */
    @Override
    public S validate(P parent, Context ctx) {
        final S state = getInitialState(parent, ctx);
        if(state != null) {
            validateForState(ctx, state);
        }
        return state;
    }

    protected void validateForState(Context ctx, final S state) {
        state.validate(ctx);
        for(Artifact<S, ? extends Artifact.State> a : getArtifacts()) {
            a.validate(state, ctx);
        }
    }

    protected abstract S getInitialState(P parent, Context ctx);

    @Override
    public S getState(P parent, Context ctx) {
        return validate(parent, ctx);
    }
}
