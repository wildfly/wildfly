/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionAttributeNamesKeyResolver}.
 * @author Paul Ferraro
 */
public class SessionAttributeNamesKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey("test");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
    }
}
