/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.undertow.sso;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.extension.undertow.security.AccountImpl;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;

/**
 * @author Paul Ferraro
 */
public class UndertowSecurityMarshallingTestCase {

    @Test
    public void testExternalizer() throws IOException {
        test(new ExternalizerTester<>(new AuthenticatedSessionExternalizer()));
    }

    @Test
    public void testProtoStream() throws IOException {
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    private static void test(Tester<AuthenticatedSession> tester) throws IOException {
        tester.test(new AuthenticatedSession(new AccountImpl("test"), HttpServletRequest.BASIC_AUTH), UndertowSecurityMarshallingTestCase::assertEquals);
        tester.test(new AuthenticatedSession(new AccountImpl(new AccountImpl("test").getPrincipal()), HttpServletRequest.CLIENT_CERT_AUTH), UndertowSecurityMarshallingTestCase::assertEquals);
        tester.test(new AuthenticatedSession(new AccountImpl(new AccountImpl("test").getPrincipal(), Collections.singleton("user"), "password"), HttpServletRequest.DIGEST_AUTH), UndertowSecurityMarshallingTestCase::assertEquals);
        tester.test(new AuthenticatedSession(new AccountImpl(new AccountImpl("test").getPrincipal(), Collections.singleton("user"), "password", new AccountImpl("original").getPrincipal()), HttpServletRequest.FORM_AUTH), UndertowSecurityMarshallingTestCase::assertEquals);
    }

    static void assertEquals(AuthenticatedSession session1, AuthenticatedSession session2) {
        Assert.assertEquals(session1.getMechanism(), session2.getMechanism());
        AccountImpl account1 = (AccountImpl) session1.getAccount();
        AccountImpl account2 = (AccountImpl) session2.getAccount();
        Assert.assertEquals(account1.getPrincipal(), account2.getPrincipal());
        Assert.assertEquals(account1.getRoles(), account2.getRoles());
        Assert.assertEquals(account1.getCredential(), account2.getCredential());
        Assert.assertEquals(account1.getOriginalPrincipal(), account2.getOriginalPrincipal());
    }
}
