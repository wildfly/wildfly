/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session;

import static org.junit.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumSet;

import org.junit.Test;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.immutable.CompositeImmutability;
import org.wildfly.clustering.ee.immutable.DefaultImmutability;
import org.wildfly.clustering.ee.immutable.DefaultImmutabilityTestCase;
import org.wildfly.clustering.web.annotation.Immutable;
import org.wildfly.common.iteration.CompositeIterable;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeImmutabilityTestCase extends DefaultImmutabilityTestCase {

    @Override
    @Test
    public void test() throws Exception {
        this.test(new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class))));
    }

    @Override
    protected void test(Immutability immutability) throws Exception {
        ImmutableObject immutableObject = new ImmutableObject();

        assertTrue(immutability.test(new ImmutableObject()));
        assertTrue(immutability.test(Collections.singleton(immutableObject)));
        assertTrue(immutability.test(Collections.singletonList(immutableObject)));
        assertTrue(immutability.test(Collections.singletonMap("1", immutableObject)));
        assertTrue(immutability.test(new AbstractMap.SimpleImmutableEntry<>("1", immutableObject)));
    }

    @Immutable
    static class ImmutableObject {
    }
}
