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

import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.CENLEN;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.CENSIG;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.CENSIZ;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.CEN_LOC_OFFSET;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.ENDLEN;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.ENDSIG;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.END_CENSTART;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.END_COMMENTLEN;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.EXTSIG;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.LOCLEN;
import static org.jboss.as.server.deployment.scanner.ZipCompletionScanner.LOCSIG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Support utility for tests of auto-deploy.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AutoDeployTestSupport {

    private static final int EXTLEN = 16;

    private final File tmpDir;
    private final File basicWar;

    private final Set<Channel> channels = new HashSet<Channel>();

    public AutoDeployTestSupport(String testId) {

        File tmp = new File(System.getProperty("java.io.tmpdir"));
        tmpDir = new File(tmp, testId);
        cleanFile(tmpDir);
        tmpDir.mkdirs();
        tmpDir.deleteOnExit();

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL webxml = tccl.getResource("basic.war/web.xml");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "basic.war");
        URL resource = tccl.getResource("basic.war/index.html");
        if (resource == null)
            throw new IllegalStateException("basic.war/index.html not found");
        war.addResource(resource, "index.html");
        war.setWebXML(webxml);

        basicWar = new File(tmpDir, "basic.war");
        basicWar.deleteOnExit();
        war.as(ZipExporter.class).exportZip(basicWar, true);
    }

    public void cleanupChannels() {
        for (Channel ch : channels) {
            try {
                ch.close();
            }
            catch (Exception ignored) {
            }
        }
    }

    public void cleanupFiles() {
        cleanFile(tmpDir);
    }

    public File getTempDir() {
        return tmpDir;
    }

    public File getBasicWar() {
        return basicWar;
    }

    public File getFile(String name) throws IOException {
        File f = new File(tmpDir, name);
        f.deleteOnExit();
        f.createNewFile();
        return f;
    }

    public FileChannel getChannel(File file, boolean read) throws IOException {
        FileChannel ch;
        if (read) {
            ch = new FileInputStream(file).getChannel();
        }
        else {
            ch = new FileOutputStream(file).getChannel();
        }
        channels.add(ch);
        return ch;
    }

    public static ByteBuffer getByteBuffer(int capacity) {
        ByteBuffer b = ByteBuffer.allocate(capacity);
        b.order(ByteOrder.LITTLE_ENDIAN);
        return b;
    }

    public static void putUnsignedShort(ByteBuffer bb, int value, int pos) {
        bb.putShort(pos, (short) (value & 0xffff));
    }

    public static void putUnsignedInt(ByteBuffer bb, long value, int pos) {
        bb.putInt(pos, (int) (value & 0xffffffffL));
    }

    /** Builds a mock zip file according to specifications */
    public void createZip(File file, int leadingBytes, boolean trailingByte, boolean extraLoc, boolean useExt, boolean useZip64) throws IOException {

        FileChannel ch = getChannel(file, false);
        try {
            ch.position(leadingBytes);
            int locPos = leadingBytes + (extraLoc ? (LOCLEN + (useExt ? EXTLEN : 0)) : 0);
            int cenPos = locPos + LOCLEN + (useExt ? EXTLEN : 0);
            int endPos = cenPos + CENLEN;
            if (extraLoc) {
                addLocFile(ch, leadingBytes, useExt);
            }
            addLocFile(ch, locPos, useExt);
            addCenDir(ch, cenPos, locPos);
            addEndRecord(ch, endPos, cenPos, trailingByte, useZip64);
        }
        finally {
            ch.close();
        }
    }

    private void addLocFile(FileChannel ch, int pos, boolean useExt) throws IOException {

        int len = useExt ? LOCLEN + EXTLEN : LOCLEN;
        ByteBuffer bb = getByteBuffer(len);

        putUnsignedInt(bb, LOCSIG, 0);
        if (useExt) {
            putUnsignedInt(bb, EXTSIG, LOCLEN);
        }
//        bb.flip();  // don't flip as we never moved the position
        ch.write(bb, pos);

    }

    private static void cleanFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    cleanFile(child);
                }
            }
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private void addCenDir(FileChannel ch, int cenPos, int locPos) throws IOException {

        ByteBuffer bb = getByteBuffer(CENLEN);

        putUnsignedInt(bb, CENSIG, 0);
        putUnsignedInt(bb, locPos, CEN_LOC_OFFSET);
        putUnsignedInt(bb, 0, CENSIZ);

//        bb.flip();  // don't flip as we never moved the position
        ch.write(bb, cenPos);

    }

    private void addEndRecord(FileChannel ch, int endPos, int cenPos, boolean trailingByte, boolean useZip64) throws IOException {

        ByteBuffer bb = getByteBuffer(ENDLEN + (trailingByte ? 1 : 0));


        putUnsignedInt(bb, ENDSIG, 0);
        putUnsignedInt(bb, useZip64 ? 0xffffffffL : cenPos, END_CENSTART);

        if (trailingByte) {
            putUnsignedShort(bb, 1, END_COMMENTLEN);
        }

//        bb.flip(); // don't flip as we never moved the position
        ch.write(bb, endPos);

    }
}
