/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.session;

import java.util.EnumSet;
import java.util.function.Predicate;

import org.wildfly.clustering.ee.CollectionImmutability;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.web.annotation.Immutable;

/**
 * @author Paul Ferraro
 */
public enum SessionAttributeImmutability implements Predicate<Object> {
    JDK() {
        @Override
        public boolean test(Object object) {
            // Skip Collection test, we override this below to extend the immutability test for collection elements.
            for (Immutability immutability : EnumSet.complementOf(EnumSet.of(Immutability.COLLECTION))) {
                if (immutability.test(object)) return true;
            }
            return false;
        }
    },
    COLLECTION() {
        @Override
        public boolean test(Object object) {
            return COLLECTION_INSTANCE.test(object);
        }
    },
    ANNOTATION() {
        @Override
        public boolean test(Object object) {
            return object.getClass().isAnnotationPresent(Immutable.class);
        }
    },
    ;

    public static final Predicate<Object> INSTANCE = object -> {
        for (SessionAttributeImmutability immutability : EnumSet.allOf(SessionAttributeImmutability.class)) {
            if (immutability.test(object)) return true;
        }
        return false;
    };
    static final Predicate<Object> COLLECTION_INSTANCE = new CollectionImmutability(INSTANCE);
}
