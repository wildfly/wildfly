/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.immutable;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.wildfly.clustering.ee.Immutability;

/**
 * Decorates a series of immutability predicates to additionally test for collection immutability.
 * @author Paul Ferraro
 */
public class CompositeImmutability implements Immutability {

    private final Iterable<? extends Immutability> immutabilities;
    private final Immutability collectionImmutability;

    public CompositeImmutability(Immutability... predicates) {
        this(Arrays.asList(predicates));
    }

    public CompositeImmutability(Iterable<? extends Immutability> immutabilities) {
        this.immutabilities = immutabilities;
        this.collectionImmutability = new CollectionImmutability(this);
    }

    @Override
    public boolean test(Object object) {
        if (object == null) return true;
        // Short-circuit test if object is an array
        if (object.getClass().isArray()) {
            return Array.getLength(object) == 0;
        }
        for (Immutability immutability : this.immutabilities) {
            if (immutability.test(object)) {
                return true;
            }
        }
        return this.collectionImmutability.test(object);
    }
}
