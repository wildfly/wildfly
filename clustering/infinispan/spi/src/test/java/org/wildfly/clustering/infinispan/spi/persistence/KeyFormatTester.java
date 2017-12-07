/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence;

import static org.junit.Assert.assertTrue;

import java.util.function.BiConsumer;

import org.junit.Assert;

/**
 * Tester for a {@link KeyFormat}.
 * @author Paul Ferraro
 */
public class KeyFormatTester<K> {

    private final KeyFormat<K> format;
    private final BiConsumer<K, K> assertion;

    public KeyFormatTester(KeyFormat<K> format) {
        this(format, Assert::assertEquals);
    }

    public KeyFormatTester(KeyFormat<K> format, BiConsumer<K, K> assertion) {
        this.format = format;
        this.assertion = assertion;
    }

    public void test(K subject) {
        assertTrue(this.format.getTargetClass().isInstance(subject));

        String formatted = this.format.format(subject);

        K result = this.format.parse(formatted);

        assertTrue(this.format.getTargetClass().isInstance(result));

        this.assertion.accept(subject, result);
    }
}
