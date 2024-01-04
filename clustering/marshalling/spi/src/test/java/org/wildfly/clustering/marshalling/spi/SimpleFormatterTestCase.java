/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import static org.mockito.Mockito.*;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleFormatterTestCase {
    @Test
    public void test() {
        Function<String, Object> parser = mock(Function.class);
        Function<Object, String> formatter = mock(Function.class);
        Formatter<Object> format = new SimpleFormatter<>(Object.class, parser, formatter);

        Object object = new Object();
        String result = "foo";

        when(formatter.apply(object)).thenReturn(result);
        when(parser.apply(result)).thenReturn(object);

        new FormatterTester<>(format).test(object, Assert::assertSame);
    }
}
