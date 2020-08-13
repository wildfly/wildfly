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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.jboss.as.jdr.commands.JdrEnvironment;
import org.jboss.as.jdr.util.JdrZipFile;
import org.jboss.as.jdr.util.PatternSanitizer;
import org.jboss.as.jdr.util.XMLSanitizer;
import org.jboss.as.jdr.vfs.Filters;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.junit.Test;

public class JdrTestCase {

    @Test
    public void testJdrZipName() throws Exception {

        // create a test-files dir to store the test JDR
        File testFilesDir = new File("target/test-files");
        testFilesDir.mkdirs();

        JdrEnvironment env = new JdrEnvironment();
        env.setOutputDirectory(testFilesDir.getAbsolutePath());
        env.setJbossHome("/foo/bar/baz");
        env.setServerName("localhost");
        env.setHostControllerName("host");
        env.setProductName("wildfly");
        env.setProductVersion("20");
        env.setZip(new JdrZipFile(new JdrEnvironment(env)));

        JdrZipFile jzf = env.getZip();
        File jdrZipFile = new File(jzf.name());
        try {
            jzf.add("test1", "test1\\sub1\\sub2\\test1.txt");
            jzf.add("test2", "test2/sub1/sub2/test2.txt");
        } finally {
            jzf.close();
        }

        // Make sure the JDR zip got created
        assertTrue(jdrZipFile.exists());
        assertTrue(jzf.name().endsWith(".zip"));
        assertTrue(jzf.name().contains("host"));

        List<Closeable> mounts = null;
        try {
            VirtualFile jdrReport = VFS.getChild(jzf.name());
            mounts = recursiveMount(jdrReport);
            VirtualFile root = jdrReport.getChildren().get(0);
            String dir = "";
            for(String path : new String[] { "sos_strings" , jzf.getProductDirName(), "test1", "sub1", "sub2" }) {
                dir = dir + "/" + path;
                assertTrue(root.getChild(dir).isDirectory());
            }
            dir = "";
            for(String path : new String[] { "sos_strings" , jzf.getProductDirName(), "test2", "sub1", "sub2" }) {
                dir = dir + "/" + path;
                assertTrue(root.getChild(dir).isDirectory());
            }
            assertTrue(root.getChild("sos_strings/" + jzf.getProductDirName() + "/test1/sub1/sub2/test1.txt").isFile());
            assertTrue(root.getChild("sos_strings/" + jzf.getProductDirName() + "/test2/sub1/sub2/test2.txt").isFile());
        } finally {
            if(mounts != null) VFSUtils.safeClose(mounts);
            // Clean up the test JDR zip file
            jdrZipFile.delete();
        }
    }

    public static List<Closeable> recursiveMount(VirtualFile file) throws IOException {
        TempFileProvider provider = TempFileProvider.create("test", Executors.newSingleThreadScheduledExecutor());
        ArrayList<Closeable> mounts = new ArrayList<Closeable>();

        if (!file.isDirectory() && file.getName().matches("^.*\\.([EeWwJj][Aa][Rr]|[Zz][Ii][Pp])$")) { mounts.add(VFS.mountZip(file, file, provider)); }

        if (file.isDirectory()) { for (VirtualFile child : file.getChildren()) { mounts.addAll(recursiveMount(child)); } }

        return mounts;
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
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        XMLSanitizer s = new XMLSanitizer("//password", Filters.TRUE);
        InputStream res = s.sanitize(is);
        byte [] buf = new byte [res.available()];
        res.read(buf);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test><password/></test>", new String(buf, StandardCharsets.UTF_8));
    }

    @Test
    public void testPatternSanitizer() throws Exception {
        String propf = "password=123456";
        InputStream is = new ByteArrayInputStream(propf.getBytes(StandardCharsets.UTF_8));
        PatternSanitizer s = new PatternSanitizer("password=.*", "password=*", Filters.TRUE);
        InputStream res = s.sanitize(is);
        byte [] buf = new byte [res.available()];
        res.read(buf);
        assertEquals("password=*", new String(buf, StandardCharsets.UTF_8));
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
        VirtualFile wingood = VFS.getChild("/C:/this/is/a/test.txt");
        VirtualFile winbad = VFS.getChild("/C:/this/is/a/test.xml");
        assertTrue(filter.accepts(good));
        assertFalse(filter.accepts(bad));
        assertTrue(filter.accepts(wingood));
        assertFalse(filter.accepts(winbad));
    }

    @Test
    public void testWildcardFilterSuffixGlob() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("/this/is*");
        VirtualFile good = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad = VFS.getChild("/that/is/a/test.txt");
        VirtualFile wingood = VFS.getChild("/C:/this/is/a/test.txt");
        VirtualFile winbad = VFS.getChild("/C:/that/is/a/test.txt");
        assertTrue(filter.accepts(good));
        assertFalse(filter.accepts(bad));
        assertTrue(filter.accepts(wingood));
        assertFalse(filter.accepts(winbad));
    }

    @Test
    public void testWildcardFilterMiddleGlob() throws Exception {
        VirtualFileFilter filter = Filters.wildcard("/this*test.txt");
        VirtualFile good = VFS.getChild("/this/is/a/test.txt");
        VirtualFile bad1 = VFS.getChild("/that/is/a/test.txt");
        VirtualFile bad2 = VFS.getChild("/this/is/a/test.xml");
        VirtualFile win = VFS.getChild("/C:/this/is/a/test.txt");
        VirtualFile winbad = VFS.getChild("/C:/this/is/a/test.xml");
        assertTrue(filter.accepts(good));
        assertTrue(filter.accepts(win));
        assertFalse(filter.accepts(bad1));
        assertFalse(filter.accepts(bad2));
        assertFalse(filter.accepts(winbad));
    }

    private void safeClose(JdrZipFile zf) {
        try {
            zf.close();
        } catch (Exception ignored) { }
    }

}
