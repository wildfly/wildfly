/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jdr;

import org.jboss.as.jdr.commands.JdrEnvironment;
import org.jboss.as.jdr.util.JdrZipFile;
import org.jboss.as.jdr.util.PatternSanitizer;
import org.jboss.as.jdr.util.XMLSanitizer;
import org.jboss.as.jdr.vfs.Filters;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

public class JdrTestCase {

    @Test
    public void testJdrZipName() throws Exception {
        JdrEnvironment env = new JdrEnvironment();
        env.setJbossHome("/foo/bar/baz");
        env.setHostControllerName("host");
        JdrZipFile zf = new JdrZipFile(env);
        String name = zf.name();

        try {
            assertTrue(name.endsWith(".zip"));
            assertTrue(name.contains("host"));
        }
        finally {
            File f = new File(zf.name());
            f.delete();
        }
    }

    @Test
    public void testBlackListFilter() {
        VirtualFileFilter blf = Filters.regexBlackList();
        assertFalse(blf.accepts(VFS.getChild("/foo/bar/baz/mgmt-users.properties")));
        assertFalse(blf.accepts(VFS.getChild("/foo/bar/baz/application-users.properties")));
    }

    @Test
    public void testXMLSanitizer() throws Exception {
        String xml = "<test><password>foobar</password></test>";
        InputStream is = new ByteArrayInputStream(xml.getBytes());
        XMLSanitizer s = new XMLSanitizer("//password", Filters.TRUE);
        InputStream res = s.sanitize(is);
        byte [] buf = new byte [res.available()];
        res.read(buf);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test><password/></test>", new String(buf));
    }

    @Test
    public void testPatternSanitizer() throws Exception {
        String propf = "password=123456";
        InputStream is = new ByteArrayInputStream(propf.getBytes());
        PatternSanitizer s = new PatternSanitizer("password=.*", "password=*", Filters.TRUE);
        InputStream res = s.sanitize(is);
        byte [] buf = new byte [res.available()];
        res.read(buf);
        assertEquals("password=*", new String(buf));
    }

    @Test
    public void testWildcardFilterAcceptAnything() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("*");
        VirtualFile good = VFS.getChild("/this/is/a/test.txt");
        assertTrue(filter.accepts(good));
    }

    @Test
    public void testWildcardFilterPrefixGlob() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("*.txt");
        VirtualFile good = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad = VFS.getChild("/this/is/a/test.xml");
        assertTrue(filter.accepts(good));
        assertFalse(filter.accepts(bad));
    }

    @Test
    public void testWildcardFilterSuffixGlob() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("/this/is*");
        VirtualFile good = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad = VFS.getChild("/that/is/a/test.txt");
        assertTrue(filter.accepts(good));
        assertFalse(filter.accepts(bad));
    }

    @Test
    public void testWildcardFilterMiddleGlob() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("/this*test.txt");
        VirtualFile good = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad1 = VFS.getChild("/that/is/a/test.txt");
        VirtualFile bad2 = VFS.getChild("/this/is/a/test.xml");
        assertTrue(filter.accepts(good));
        assertFalse(filter.accepts(bad1));
        assertFalse(filter.accepts(bad2));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testWildcardFilterPrefixSingle() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("?this/is/a/test.txt");
        VirtualFileFilter filter2 = Filters.wildcard("?????/is/a/test.txt");
    }

    @Test
    public void testWildcardFilterPostfixSingle() throws Exception {
        VirtualFileFilter filter1 = Filters.wildcard("/this/is/a/test.tx?");
        VirtualFile good1 = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad1 = VFS.getChild("/that/is/a/test.dat");
        assertTrue(filter1.accepts(good1));
        assertFalse(filter1.accepts(bad1));

        VirtualFileFilter filter2 = Filters.wildcard("/this/is/a/test.???");
        VirtualFile good2 = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad2 = VFS.getChild("/that/is/a/blah.txt");
        assertTrue(filter2.accepts(good2));
        assertFalse(filter2.accepts(bad2));

    }

    @Test
    public void testWildcardFilterMiddleSingle() throws Exception {
        VirtualFileFilter filter1 = Filters.wildcard("/this/???/a/test.txt");
        VirtualFile good1 = VFS.getChild("/this/iss/a/test.txt");
        VirtualFile bad1 = VFS.getChild("/that/was/no/test.dat");
        assertTrue(filter1.accepts(good1));
        assertFalse(filter1.accepts(bad1));

        VirtualFileFilter filter2 = Filters.wildcard("/????/is/a/????.txt");
        VirtualFile good2 = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad2 = VFS.getChild("/that/is/no/test.txt");
        assertTrue(filter2.accepts(good2));
        assertFalse(filter2.accepts(bad2));

    }
}
