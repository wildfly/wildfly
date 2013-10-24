/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;

import javax.security.auth.Subject;

import org.jboss.as.controller.security.InetAddressPrincipal;
import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmRole;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.core.security.SimplePrincipal;
import org.junit.Test;

/**
 * Test case for the utility to write and read subjects.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SubjectProtocolUtilTestCase {

    @Test
    public void testSupportedTypes() throws Exception {
        Collection<Principal> toSend = new HashSet<Principal>();
        Collection<Principal> toreceive = new HashSet<Principal>();

        add(new RealmUser("NamedOnlyUser"), toSend, toreceive);
        add(new RealmUser("TestRealm", "TestUser"), toSend, toreceive);
        add(new RealmGroup("NamedOnlyGroup"), toSend, toreceive);
        add(new RealmGroup("TestRealm", "TestGroup"), toSend, toreceive);
        add(new RealmRole("RealmRole"), toSend, toreceive);

        InetAddress testAddress = InetAddress.getByAddress("localhost", new byte[] { 0x7F, 0x00, 0x00, 0x01 });
        add(new InetAddressPrincipal(testAddress), toSend, toreceive);

        assertEquals("To send count", 6, toSend.size());
        assertEquals("To receive count", 6, toreceive.size());

        performTest(toSend, toreceive);
    }

    @Test
    public void testUnSupportedTypes() throws Exception {
        Collection<Principal> toSend = new HashSet<Principal>();
        Collection<Principal> toreceive = new HashSet<Principal>();

        add(new RealmUser("NamedOnlyUser"), toSend, toreceive);
        add(new RealmUser("TestRealm", "TestUser"), toSend, toreceive);
        add(new RealmGroup("NamedOnlyGroup"), toSend, toreceive);
        add(new SimplePrincipal("SimplePrincipal"), toSend);
        add(new RealmGroup("TestRealm", "TestGroup"), toSend, toreceive);
        add(new RealmRole("RealmRole"), toSend, toreceive);

        InetAddress testAddress = InetAddress.getByAddress("localhost", new byte[] { 0x7F, 0x00, 0x00, 0x01 });
        add(new InetAddressPrincipal(testAddress), toSend, toreceive);

        assertEquals("To send count", 7, toSend.size());
        assertEquals("To receive count", 6, toreceive.size());

        performTest(toSend, toreceive);
    }

    private void add(Principal principal, Collection<Principal>... collections) {
        for (Collection<Principal> current : collections) {
            assertTrue("Principal added", current.add(principal));
        }
    }

    private void performTest(Collection<Principal> original, Collection<Principal> expected) throws IOException {
        Subject toSend = new Subject();
        toSend.getPrincipals().addAll(original);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        SubjectProtocolUtil.write(dos, toSend);
        dos.close();
        baos.close();

        byte[] sent = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(sent);
        DataInputStream dis = new DataInputStream(bais);

        Subject received = SubjectProtocolUtil.read(dis);

        for (Principal current : received.getPrincipals()) {
            assertTrue("Principal received was in expected list.", expected.remove(current));
        }
        assertTrue("All expected principals received", expected.isEmpty());
    }

}
