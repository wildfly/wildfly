/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.fine;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;

/**
 * Unit test for {@link SessionAttributeKey}.
 * @author Paul Ferraro
 */
public class SessionAttributeKeyTestCase {

    @Test
    public void test() throws IOException {
        SessionAttributeKey key = new SessionAttributeKey("ABC123", UUID.randomUUID());
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new SessionAttributeKeyFormatter()).test(key);
    }
}
