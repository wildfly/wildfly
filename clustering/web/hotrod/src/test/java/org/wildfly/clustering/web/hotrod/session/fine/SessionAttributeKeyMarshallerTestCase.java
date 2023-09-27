/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionAttributeKeyResolver}.
 * @author Paul Ferraro
 */
public class SessionAttributeKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionAttributeKey key = new SessionAttributeKey("test", UUID.randomUUID());
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
    }
}
