/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.naming.java.permission;

import static org.junit.Assert.*;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;

import org.junit.Test;

/**
 * Big ol' JNDI permission test case.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class JndiPermissionTestCase {
    @Test
    public void testNameImplies() {
        // check the compat <<ALL BINDINGS>> name
        assertEquals(new JndiPermission("<<ALL BINDINGS>>", "*"), new JndiPermission("-", "*"));

        // check the root - name
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("-", "*")));
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("", "*")));
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("foo", "*")));
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("/foo", "*")));
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("foo/", "*")));
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("foo/bar/baz/zap", "*")));
        assertTrue(new JndiPermission("-", "*").implies(new JndiPermission("java:foo", "*")));

        // check the non-root - name
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("/-", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("/", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("//", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("////", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("/foo", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("/foo", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("/foo/", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("/foo/bar/baz/zap", "*")));
        assertTrue(new JndiPermission("/-", "*").implies(new JndiPermission("java:/foo", "*")));

        assertTrue(new JndiPermission("foo/-", "*").implies(new JndiPermission("foo/-", "*")));
        assertTrue(new JndiPermission("foo/-", "*").implies(new JndiPermission("foo/foo", "*")));
        assertTrue(new JndiPermission("foo/-", "*").implies(new JndiPermission("foo/foo", "*")));
        assertTrue(new JndiPermission("foo/-", "*").implies(new JndiPermission("foo/foo/", "*")));
        assertTrue(new JndiPermission("foo/-", "*").implies(new JndiPermission("foo/foo/bar/baz/zap", "*")));
        assertTrue(new JndiPermission("foo/-", "*").implies(new JndiPermission("java:foo/foo", "*")));

        // check the * name
        assertTrue(new JndiPermission("*", "*").implies(new JndiPermission("", "*")));
        assertTrue(new JndiPermission("*", "*").implies(new JndiPermission("foo", "*")));
        assertFalse(new JndiPermission("*", "*").implies(new JndiPermission("foo/bar", "*")));
        assertFalse(new JndiPermission("*", "*").implies(new JndiPermission("foo/", "*")));
        assertFalse(new JndiPermission("*", "*").implies(new JndiPermission("/foo", "*")));
        assertTrue(new JndiPermission("*/*", "*").implies(new JndiPermission("/foo", "*")));
        assertTrue(new JndiPermission("/*", "*").implies(new JndiPermission("/foo", "*")));
        assertTrue(new JndiPermission("*/foo", "*").implies(new JndiPermission("/foo", "*")));

        // check java: support
        assertEquals(new JndiPermission("java:", "*"), new JndiPermission("", "*"));
        assertEquals(new JndiPermission("java:/", "*"), new JndiPermission("/", "*"));
        assertEquals(new JndiPermission("java:-", "*"), new JndiPermission("-", "*"));
        assertEquals(new JndiPermission("java:*", "*"), new JndiPermission("*", "*"));
    }

    @Test
    public void testActions() {
        assertEquals(new JndiPermission("foo", "*"), new JndiPermission("foo", "all"));
        assertEquals(new JndiPermission("foo", "*"), new JndiPermission("foo", "lookup,bind,rebind,unbind,list,listBindings,createSubcontext,destroySubcontext,addNamingListener"));
        assertEquals(new JndiPermission("foo", "*"), new JndiPermission("foo", "unbind,list,listBindings,createSubcontext,destroySubcontext,addNamingListener,lookup,bind,rebind"));

        assertTrue(new JndiPermission("foo", "*").implies(new JndiPermission("foo", "lookup")));
        assertTrue(new JndiPermission("foo", "").implies(new JndiPermission("foo", "")));
        assertTrue(new JndiPermission("foo", "*").implies(new JndiPermission("foo", "")));
        assertFalse(new JndiPermission("foo", "").implies(new JndiPermission("foo", "bind")));
        assertTrue(new JndiPermission("foo", "").withActions("bind").implies(new JndiPermission("foo", "bind")));
        assertFalse(new JndiPermission("foo", "unbind").withoutActions("unbind").implies(new JndiPermission("foo", "unbind")));
    }

    @Test
    public void testCollection() {
        final PermissionCollection permissionCollection = new JndiPermission("", "").newPermissionCollection();
        Enumeration<Permission> e;
        permissionCollection.add(new JndiPermission("foo/bar", "lookup,bind"));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind")));
        assertFalse(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind,unbind")));
        assertFalse(permissionCollection.implies(new JndiPermission("foo/bar", "unbind")));
        assertNotNull(e = permissionCollection.elements());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("foo/bar", "lookup,bind"), e.nextElement());
        assertFalse(e.hasMoreElements());
        permissionCollection.add(new JndiPermission("foo/bar", "unbind"));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind")));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind,unbind")));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "unbind")));
        assertNotNull(e = permissionCollection.elements());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("foo/bar", "lookup,bind,unbind"), e.nextElement());
        assertFalse(e.hasMoreElements());
        permissionCollection.add(new JndiPermission("-", "lookup"));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind")));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind,unbind")));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "unbind")));
        assertTrue(permissionCollection.implies(new JndiPermission("baz/zap", "lookup")));
        assertTrue(permissionCollection.implies(new JndiPermission("", "lookup")));
        assertFalse(permissionCollection.implies(new JndiPermission("baz/zap", "lookup,bind,unbind")));
        assertFalse(permissionCollection.implies(new JndiPermission("baz/zap", "unbind")));
        assertNotNull(e = permissionCollection.elements());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("foo/bar", "lookup,bind,unbind"), e.nextElement());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("-", "lookup"), e.nextElement());
        assertFalse(e.hasMoreElements());
        permissionCollection.add(new JndiPermission("-", "bind,unbind"));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind")));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "lookup,bind,unbind")));
        assertTrue(permissionCollection.implies(new JndiPermission("foo/bar", "unbind")));
        assertTrue(permissionCollection.implies(new JndiPermission("baz/zap", "lookup")));
        assertTrue(permissionCollection.implies(new JndiPermission("", "lookup")));
        assertTrue(permissionCollection.implies(new JndiPermission("baz/zap", "lookup,bind,unbind")));
        assertTrue(permissionCollection.implies(new JndiPermission("baz/zap", "unbind")));
        assertNotNull(e = permissionCollection.elements());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("-", "lookup,bind,unbind"), e.nextElement());
        assertFalse(e.hasMoreElements());
    }

    @Test
    public void testSecurity() {
        assertEquals(new JndiPermission("-", Integer.MAX_VALUE).getActionBits(), JndiPermission.ACTION_ALL);
        assertEquals(new JndiPermission("-", Integer.MAX_VALUE), new JndiPermission("-", "*"));
    }

    @Test
    public void testSerialization() {
        final JndiPermission jndiPermission = new JndiPermission("foo/blap/-", "bind,lookup");
        assertEquals(jndiPermission, ((SerializedJndiPermission)jndiPermission.writeReplace()).readResolve());
    }

    @Test
    public void testCollectionSecurity() {
        final PermissionCollection permissionCollection = new JndiPermission("", "").newPermissionCollection();
        permissionCollection.add(new JndiPermission("foo/bar", "unbind,rebind"));
        permissionCollection.setReadOnly();
        try {
            permissionCollection.add(new JndiPermission("fob/baz", "unbind,rebind"));
            fail("Expected exception");
        } catch (SecurityException ignored) {
        }
    }

    @Test
    public void testCollectionSerialization() {
        final PermissionCollection permissionCollection = new JndiPermission("", "").newPermissionCollection();
        permissionCollection.add(new JndiPermission("foo/bar", "createSubcontext,rebind"));
        permissionCollection.add(new JndiPermission("foo", "addNamingListener"));
        permissionCollection.add(new JndiPermission("-", "lookup,rebind"));
        final PermissionCollection other = (PermissionCollection) ((SerializedJndiPermissionCollection) ((JndiPermissionCollection)permissionCollection).writeReplace()).readResolve();
        Enumeration<Permission> e;
        assertNotNull(e = other.elements());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("foo/bar", "createSubcontext,rebind"), e.nextElement());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("foo", "addNamingListener"), e.nextElement());
        assertTrue(e.hasMoreElements());
        assertEquals(new JndiPermission("-", "lookup,rebind"), e.nextElement());
        assertFalse(e.hasMoreElements());
    }
}
