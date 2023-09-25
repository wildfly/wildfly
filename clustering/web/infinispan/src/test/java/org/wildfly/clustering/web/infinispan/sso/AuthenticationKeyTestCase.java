/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;

/**
 * Unit test for {@link AuthenticationKey}.
 * @author Paul Ferraro
 */
public class AuthenticationKeyTestCase {

    @Test
    public void test() throws IOException {
        AuthenticationKey key = new AuthenticationKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new AuthenticationKeyFormatter()).test(key);
    }
}
