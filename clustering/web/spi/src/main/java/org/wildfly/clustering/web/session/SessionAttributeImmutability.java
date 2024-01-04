/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.immutable.AnnotationImmutability;

import org.wildfly.clustering.web.annotation.Immutable;

/**
 * Session attribute immutability tests.
 * @author Paul Ferraro
 */
public enum SessionAttributeImmutability implements Immutability {
    ANNOTATION(new AnnotationImmutability(Immutable.class)),
    ;
    private final Immutability immutability;

    SessionAttributeImmutability(Immutability immutability) {
        this.immutability = immutability;
    }

    @Override
    public boolean test(Object object) {
        return this.immutability.test(object);
    }
}
