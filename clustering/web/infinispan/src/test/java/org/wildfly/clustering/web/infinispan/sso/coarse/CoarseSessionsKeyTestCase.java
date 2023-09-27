/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso.coarse;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;

/**
 * Unit test for {@link CoarseSessionsKey}.
 * @author Paul Ferraro
 */
public class CoarseSessionsKeyTestCase {

    @Test
    public void test() throws IOException {
        CoarseSessionsKey key = new CoarseSessionsKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new CoarseSessionsKeyFormatter()).test(key);
    }
}
