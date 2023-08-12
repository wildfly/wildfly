/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataKey;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataKeyFormatter;

/**
 * Unit test for {@link SessionCreationMetaDataKey}.
 * @author Paul Ferraro
 */
public class SessionMetaDataKeyTestCase {

    @Test
    public void test() throws IOException {
        SessionMetaDataKey key = new SessionMetaDataKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new SessionMetaDataKeyFormatter()).test(key);
    }
}
