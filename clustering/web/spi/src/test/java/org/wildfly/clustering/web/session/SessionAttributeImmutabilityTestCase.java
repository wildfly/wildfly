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
