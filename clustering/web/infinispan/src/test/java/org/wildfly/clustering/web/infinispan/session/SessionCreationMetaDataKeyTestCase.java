/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;

/**
 * Unit test for {@link SessionCreationMetaDataKey}.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataKeyTestCase {

    @Test
    public void test() throws IOException {
        SessionCreationMetaDataKey key = new SessionCreationMetaDataKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new SessionCreationMetaDataKeyFormatter()).test(key);
    }
}
