/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.metadata;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionCreationMetaDataKeyResolver}.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionCreationMetaDataKey key = new SessionCreationMetaDataKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
    }
}
