/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jboss.as.patching.PatchInfo;

/**
 * @author Emanuel Muckenhuber
 */
public final class PatchUtils {

    static final int DEFAULT_BUFFER_SIZE = 65536;
    public static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
        public void write(int b) throws IOException {
            //
        }
    };

    public static String readRef(final File file) throws IOException {
        if(! file.exists()) {
            return PatchInfo.BASE;
        }
        final InputStream is = new FileInputStream(file);
        try {
            return readRef(is);
        } finally {
            safeClose(is);
        }
    }

    public static List<String> readRefs(final File file) throws IOException {
        if(! file.exists()) {
            return Collections.emptyList();
        }
        final InputStream is = new FileInputStream(file);
        try {
            return readRefs(is);
        } finally {
            safeClose(is);
        }
    }

    static String readRef(final InputStream is) throws IOException {
        final StringBuffer buffer = new StringBuffer();
        readLine(is, buffer);
        return buffer.toString();
    }

    static List<String> readRefs(final InputStream is) throws IOException {
        final List<String> refs = new ArrayList<String>();
        final StringBuffer buffer = new StringBuffer();
        do {
            if(buffer.length() > 0) {
                final String ref = buffer.toString().trim();
                if(ref.length() > 0) {
                    refs.add(ref);
                }
            }
        } while(readLine(is, buffer));
        return refs;
    }

    public static void writeRef(final File file, final String ref) throws IOException {
        final OutputStream os = new FileOutputStream(file);
        try {
            writeLine(os, ref);
            os.flush();
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public static void writeRefs(final File file, final List<String> refs) throws IOException {
        final OutputStream os = new FileOutputStream(file);
        try {
            writeRefs(os, refs);
            os.flush();
            os.close();
        } finally {
            safeClose(os);
        }
    }

    static void writeRefs(final OutputStream os, final List<String> refs) throws IOException {
        for(final String ref : refs) {
            writeLine(os, ref);
        }
    }

    static void writeLine(final OutputStream os, final String s) throws IOException {
        os.write(s.getBytes());
        os.write('\n');
    }

    static boolean readLine(InputStream is, StringBuffer buffer) throws IOException {
        buffer.setLength(0);
        int c;
        for(;;) {
            c = is.read();
            switch(c) {
                case '\t':
                case '\r':
                    break;
                case -1: return false;
                case '\n': return true;
                default: buffer.append((char) c);
            }
        }
    }

    // FIXME do we need to i18nize the timestamp?
    static String generateTimestamp() {
        return DateFormat.getInstance().format(new Date());
    }
}
