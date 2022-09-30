/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
