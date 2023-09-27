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
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.cache.CachedIdentity;

/**
 * @author Paul Ferraro
 */
public class IdentityContainerMarshallerTestCase {

    @Test
    public void testProtoStream() throws IOException {
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    @Test
    public void testJBoss() throws IOException {
        test(JBossMarshallingTesterFactory.INSTANCE.createTester());
    }

    private static void test(Tester<IdentityContainer> tester) throws IOException {
        Principal principal = new NamePrincipal("name");
        tester.test(new IdentityContainer(new CachedIdentity(HttpServletRequest.BASIC_AUTH, false, principal), "foo"), IdentityContainerMarshallerTestCase::assertEquals);
        tester.test(new IdentityContainer(new CachedIdentity(HttpServletRequest.BASIC_AUTH, false, principal), null), IdentityContainerMarshallerTestCase::assertEquals);
    }

    static void assertEquals(IdentityContainer container1, IdentityContainer container2) {
        CachedIdentityMarshallingTestCase.assertEquals(container1.getSecurityIdentity(), container2.getSecurityIdentity());
        Assert.assertEquals(container1.getAuthType(), container2.getAuthType());
    }
}
