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
import java.util.Collections;
import java.util.List;


/**
 * @author Alexey Loubyansky
 *
 */
public abstract class ArtifactCollectionState<S extends Artifact.State> implements Artifact.State {

    private List<S> list = Collections.emptyList();
    private int i;

    protected S newItem() {
        S state = createItem();
        add(state);
        return state;
    }

    protected abstract S createItem();

    private void add(S item) {
        switch(list.size()) {
            case 0:
                list = Collections.singletonList(item);
                break;
            case 1:
                final List<S> tmp = list;
                list = new ArrayList<S>();
                list.add(tmp.get(0));
            default:
                list.add(item);
        }
    }

    protected S getState() {
        return i < list.size() ? list.get(i) : null;
    }

    void resetIndex() {
        this.i = 0;
    }

    boolean hasNext() {
        return i < list.size();
    }

    void next() {
        ++i;
    }

    @Override
    public void validate(Context ctx) {
        final S state = getState();
        if(state != null) {
            state.validate(ctx);
        }
    }
}
