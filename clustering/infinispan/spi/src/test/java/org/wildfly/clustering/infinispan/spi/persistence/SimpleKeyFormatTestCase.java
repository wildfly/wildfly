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

import static org.mockito.Mockito.*;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleKeyFormatTestCase {
    @Test
    public void test() {
        Function<String, Object> parser = mock(Function.class);
        Function<Object, String> formatter = mock(Function.class);
        KeyFormat<Object> format = new SimpleKeyFormat<>(Object.class, parser, formatter);

        Object object = new Object();
        String result = "foo";

        when(formatter.apply(object)).thenReturn(result);
        when(parser.apply(result)).thenReturn(object);

        new KeyFormatTester<>(format, Assert::assertSame).test(object);
    }
}
