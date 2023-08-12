/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.metadata;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionAccessMetaDataKeyResolver}.
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionAccessMetaDataKey key = new SessionAccessMetaDataKey("test");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
    }
}
