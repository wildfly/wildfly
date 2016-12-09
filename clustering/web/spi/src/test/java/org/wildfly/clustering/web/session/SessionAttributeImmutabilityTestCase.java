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

import java.util.Collections;
import java.util.function.Predicate;

import org.junit.Test;
import org.wildfly.clustering.ee.ImmutabilityTestCase;
import org.wildfly.clustering.web.annotation.Immutable;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeImmutabilityTestCase extends ImmutabilityTestCase {

    @Override
    @Test
    public void test() throws Exception {
        this.test(SessionAttributeImmutability.INSTANCE);
    }

    @Override
    protected void test(Predicate<Object> immutability) throws Exception {
        super.test(immutability);

        assertTrue(immutability.test(new ImmutableObject()));
        assertTrue(immutability.test(Collections.singleton(new ImmutableObject())));
        assertTrue(immutability.test(Collections.singletonList(new ImmutableObject())));
        assertTrue(immutability.test(Collections.singletonMap("1", new ImmutableObject())));
    }

    @Immutable
    static class ImmutableObject {
    }
}
