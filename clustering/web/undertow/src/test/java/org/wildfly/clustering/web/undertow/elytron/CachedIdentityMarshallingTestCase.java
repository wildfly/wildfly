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
