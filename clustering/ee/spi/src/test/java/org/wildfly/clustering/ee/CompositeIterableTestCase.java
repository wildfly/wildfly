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

package org.wildfly.clustering.ee;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link CompositeIterable}.
 * @author Paul Ferraro
 */
public class CompositeIterableTestCase {

    @Test
    public void test() {
        Iterable<Integer> expected = IntStream.range(0, 10).mapToObj(Integer::valueOf).collect(Collectors.toList());

        test(expected, new CompositeIterable<>(Arrays.asList(0, 1, 2, 3, 4), Arrays.asList(5, 6, 7, 8, 9)));
        test(expected, new CompositeIterable<>(Arrays.asList(0, 1), Arrays.asList(2, 3), Arrays.asList(4, 5), Arrays.asList(6, 7), Arrays.asList(8, 9)));
        test(expected, new CompositeIterable<>(Collections.emptyList(), expected, Collections.emptyList()));
    }

    static void test(Iterable<Integer> expected, Iterable<Integer> subject) {
        Assert.assertEquals(expected.hashCode(), subject.hashCode());
        Assert.assertEquals(expected.toString(), subject.toString());

        Iterator<Integer> subjectIterator = subject.iterator();
        Iterator<Integer> expectedIterator = expected.iterator();
        while (expectedIterator.hasNext()) {
            Assert.assertTrue(subjectIterator.hasNext());
            Assert.assertEquals(expectedIterator.next(), subjectIterator.next());
        }
        Assert.assertFalse(subjectIterator.hasNext());
    }
}
