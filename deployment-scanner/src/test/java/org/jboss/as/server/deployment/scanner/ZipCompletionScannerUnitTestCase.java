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
package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.server.deployment.scanner.AutoDeployTestSupport.getByteBuffer;
import static org.jboss.as.server.deployment.scanner.AutoDeployTestSupport.putUnsignedShort;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jboss.as.server.deployment.scanner.ZipCompletionScanner.NonScannableZipException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link ZipCompletionScanner}
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ZipCompletionScannerUnitTestCase {

    private static AutoDeployTestSupport testSupport;

    @BeforeClass
    public static void setupClass() {

        testSupport = new AutoDeployTestSupport(ZipCompletionScannerUnitTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void teardownClass() {
        if (testSupport != null) {
            testSupport.cleanupFiles();
        }
    }

    @After
    public void tearDown() {
        testSupport.cleanupChannels();
    }

    @Test
    public void testBasicWar() throws Exception {
        Assert.assertTrue(ZipCompletionScanner.isCompleteZip(testSupport.getBasicWar()));
    }

    @Test
    public void testEmptyFile() throws Exception {
        File empty = testSupport.getFile("empty.jar");
        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(empty));
    }

    @Test
    public void testMaxScan() throws Exception {
        int size = (1 << 16) + 22;
        File maxscan = testSupport.getFile("maxscan.jar");
        FileChannel ch = testSupport.getChannel(maxscan, false);
        ByteBuffer bb = ByteBuffer.allocate(1);
        bb.put((byte)0);
        bb.flip();
        ch.write(bb, size -1);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(maxscan));

        File maxscanplus = testSupport.getFile("maxscanplus.jar");
        ch = testSupport.getChannel(maxscanplus, false);
        bb.flip();
        ch.write(bb, size);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(maxscanplus));

        File maxscanminus = testSupport.getFile("maxscanminus.jar");
        ch = testSupport.getChannel(maxscanminus, false);
        bb.flip();
        ch.write(bb, size - 2);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(maxscanplus));
    }

    @Test
    public void testLeadingBytes() throws Exception {

        File leading = testSupport.getFile("leadingbyte.jar");

        testSupport.createZip(leading, 1, false, false, false, false);

        Assert.assertTrue(ZipCompletionScanner.isCompleteZip(leading));
    }

    @Test
    public void testTrailingByte() throws Exception {

        File trailing = testSupport.getFile("trailingbyte.jar");

        FileChannel in = testSupport.getChannel(testSupport.getBasicWar(), true);

        FileChannel out = testSupport.getChannel(trailing, false);

        ByteBuffer bb = getByteBuffer((int) in.size());
        in.read(bb);
        putUnsignedShort(bb, 1, (int) in.size() - 2);
        bb.flip();
        out.write(bb);

        long size = out.size();

        bb = ByteBuffer.allocate(1);
        bb.put((byte)0);
        bb.flip();
        out.write(bb, size);

        Assert.assertTrue(ZipCompletionScanner.isCompleteZip(trailing));
    }

    @Test
    public void testTrailingBytes() throws Exception {

        File trailing = testSupport.getFile("trailingbytes.jar");

        FileChannel in = testSupport.getChannel(testSupport.getBasicWar(), true);

        FileChannel out = testSupport.getChannel(trailing, false);

        int maxShort = (1 << 16) -1;
        ByteBuffer bb = getByteBuffer((int) in.size());
        in.read(bb);
        putUnsignedShort(bb, maxShort , (int) in.size() - 2);
        bb.flip();
        out.write(bb);

        long size = out.size();

        bb = ByteBuffer.allocate(1);
        bb.put((byte)0);
        bb.flip();
        out.write(bb, size + maxShort -1);

        Assert.assertTrue(ZipCompletionScanner.isCompleteZip(trailing));
    }

    @Test
    public void testLeadingAndTrailingBytes() throws Exception {

        File file = testSupport.getFile("leadingtrailing.jar");

        testSupport.createZip(file, 1, true, false, false, false);

        Assert.assertTrue(ZipCompletionScanner.isCompleteZip(file));
    }

    @Test
    public void testTruncatedEndRecord() throws Exception {

        File truncated = testSupport.getFile("truncated1.jar");

        FileChannel in = testSupport.getChannel(testSupport.getBasicWar(), true);

        FileChannel out = testSupport.getChannel(truncated, false);

        // Write out all but 1 byte
        ByteBuffer bb = getByteBuffer((int) in.size() - 1);
        in.read(bb);
        bb.flip();
        out.write(bb);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(truncated));

        truncated = testSupport.getFile("truncated2.jar");

        out = testSupport.getChannel(truncated, false);

        // Write out just past the end of central dir record signature
        bb = getByteBuffer((int) in.size() - 18);
        in.read(bb, 0);
        bb.flip();
        out.write(bb);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(truncated));

    }

    @Test
    public void testExtendedDataDescriptor() throws Exception {

        File file = testSupport.getFile("extdata.jar");

        testSupport.createZip(file, 0, false, false, true, false);

        Assert.assertTrue(ZipCompletionScanner.isCompleteZip(file));
    }

    @Test
    public void testFindNestedCentralDir() throws Exception {

        File file = testSupport.getFile("findnested.jar");

        testSupport.createZip(file, 0, false, true, false, false);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(file));
    }

    @Test
    public void testFindNestedCentralDirWithExt() throws Exception {

        File file = testSupport.getFile("findnestedext.jar");

        testSupport.createZip(file, 0, false, true, true, false);

        Assert.assertFalse(ZipCompletionScanner.isCompleteZip(file));
    }

    @Test
    public void testFindNestedCentralDirWithLeadingBytes() throws Exception {

        File file = testSupport.getFile("findnestedleading.jar");

        testSupport.createZip(file, 1, false, true, false, false);

        try {
            ZipCompletionScanner.isCompleteZip(file);
            Assert.fail("Scan of jar with nested content and leading bytes did not fail");
        }
        catch (NonScannableZipException good) {
        }
    }

    @Test
    public void testFindNestedCentralDirWithExtAndLeadingBytes() throws Exception {

        File file = testSupport.getFile("findnestedleadingext.jar");

        testSupport.createZip(file, 1, false, true, true, false);

        try {
            ZipCompletionScanner.isCompleteZip(file);
            Assert.fail("Scan of jar with nested content and leading bytes did not fail");
        }
        catch (NonScannableZipException good) {
        }
    }

    // This test should be reworked if Zip64 support is added
    @Test
    public void testZip64() throws Exception {

        File zip = testSupport.getFile("leadingbyte.jar");

        testSupport.createZip(zip, 0, false, false, false, true);

        try {
            ZipCompletionScanner.isCompleteZip(zip);
            Assert.fail("Scan of Zip64 file did not fail");
        }
        catch (NonScannableZipException good) {
        }
    }

}
