/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.cache.CachedIdentity;

/**
 * @author Paul Ferraro
 */
public class CachedIdentityMarshallingTestCase {

    @Test
    public void testProtoStream() throws IOException {
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    @Test
    public void testJBoss() throws IOException {
        test(JBossMarshallingTesterFactory.INSTANCE.createTester());
    }

    private static void test(Tester<CachedIdentity> tester) throws IOException {
        Principal principal = new NamePrincipal("name");
        tester.test(new CachedIdentity(HttpServletRequest.BASIC_AUTH, false, principal), CachedIdentityMarshallingTestCase::assertEquals);
        tester.test(new CachedIdentity(HttpServletRequest.CLIENT_CERT_AUTH, false, principal), CachedIdentityMarshallingTestCase::assertEquals);
        tester.test(new CachedIdentity(HttpServletRequest.DIGEST_AUTH, false, principal), CachedIdentityMarshallingTestCase::assertEquals);
        tester.test(new CachedIdentity(HttpServletRequest.FORM_AUTH, false, principal), CachedIdentityMarshallingTestCase::assertEquals);
        tester.test(new CachedIdentity("Programmatic", true, principal), CachedIdentityMarshallingTestCase::assertEquals);
    }

    static void assertEquals(CachedIdentity auth1, CachedIdentity auth2) {
        Assert.assertEquals(auth1.getMechanismName(), auth2.getMechanismName());
        Assert.assertEquals(auth1.getName(), auth2.getName());
        Assert.assertEquals(auth1.isProgrammatic(), auth2.isProgrammatic());
    }
}
