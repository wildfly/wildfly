/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.Test;

import io.undertow.server.session.SessionIdGenerator;

/**
 * Unit test for {@link IdentifierFactoryAdapter}
 *
 * @author Paul Ferraro
 */
public class IdentifierFactoryAdapterTestCase {
    private final SessionIdGenerator generator = mock(SessionIdGenerator.class);
    private final Supplier<String> factory = new IdentifierFactoryAdapter(this.generator);

    @Test
    public void test() {
        String expected = "expected";
        when(this.generator.createSessionId()).thenReturn(expected);

        String result = this.factory.get();

        assertSame(expected, result);
    }
}
