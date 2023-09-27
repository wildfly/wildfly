/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.fine;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;

/**
 * Unit test for {@link SessionAttributeNamesKey}.
 * @author Paul Ferraro
 */
public class SessionAttributeNamesKeyTestCase {

    @Test
    public void test() throws IOException {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new SessionAttributeNamesKeyFormatter()).test(key);
    }
}
