/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.attributes;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

public class SessionAttributesKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionAttributesKey key = new SessionAttributesKey("test");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
    }
}
