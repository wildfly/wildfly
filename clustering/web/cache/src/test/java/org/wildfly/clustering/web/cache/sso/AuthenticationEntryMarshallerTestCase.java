/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.sso;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link AuthenticationEntryExternalizer}.
 * @author Paul Ferraro
 */
public class AuthenticationEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTester<AuthenticationEntry<String, Object>> tester = ProtoStreamTesterFactory.createTester(List.of(new SSOSerializationContextInitializer()));
        tester.test(new AuthenticationEntry<>("username"), AuthenticationEntryMarshallerTestCase::assertEquals);
    }

    static void assertEquals(AuthenticationEntry<String, Object> entry1, AuthenticationEntry<String, Object> entry2) {
        Assert.assertEquals(entry1.getAuthentication(), entry2.getAuthentication());
    }
}
