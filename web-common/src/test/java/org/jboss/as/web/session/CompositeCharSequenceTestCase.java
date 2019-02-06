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

package org.jboss.as.web.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.stream.IntStream;

import org.junit.Test;

/**
 * Unit test for {@link CompositeCharSequence}.
 * @author Paul Ferraro
 */
public class CompositeCharSequenceTestCase {
    @Test
    public void test() {
        CharSequence sequence = new CompositeCharSequence("01", "23", "45");
        assertEquals(6, sequence.length());
        assertEquals("012345", sequence.toString());

        for (int i = -1; i < 7; ++i) {
            try {
                char result = sequence.charAt(i);
                if ((i < 0) || (i >= 6)) {
                    fail(String.format("charAt(%d) returned '%s', but IndexOutOfBoundsException was expected", i, result));
                }
                assertEquals('0' + i, result);
            } catch (IndexOutOfBoundsException e) {
                if ((i >= 0) && (i < 6)) {
                    fail(String.format("Unexpected IndexOutOfBoundsException during charAt(%d)", i));
                }
            }
        }

        for (int i = -1; i < 7; ++i) {
            for (int j = -1; j <= 7; ++j) {
                try {
                    CharSequence result = sequence.subSequence(i, j);
                    if ((i < 0) || (i > j) || (j > 6)) {
                        fail(String.format("subSequence(%d, %d) returned '%s', but IndexOutOfBoundsException was expected", i, j, result.toString()));
                    }
                    StringBuilder expected = new StringBuilder(j - i);
                    IntStream.range(i, j).forEach(value -> expected.append((char) ('0' + value)));
                    assertEquals(expected.toString(), result.toString());
                } catch (IndexOutOfBoundsException e) {
                    if ((i >= 0) && (j <= 6) && (i <= j)) {
                        fail(String.format("Unexpected IndexOutOfBoundsException during subSequence(%d, %d)", i, j));
                    }
                }
            }
        }
    }
}
