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

package org.jboss.as.patching.runner;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jboss.as.patching.IoUtils;

/**
 * Cripple a JAR or other zip file by flipping a bit in the end of central directory record
 * This process can be reversed by flipping the bit back.
 * Useful for rendering JARs non-executable.
 *
 * @author David Jorm
 */
class PatchModuleInvalidationUtils {

    private static final byte INVALIDATION_VALUE = 7;
    private static final byte RESTORE_VALUE = 6;

    private static final byte[] GOOD_SIGNATURE = new byte[] {5, 6};
    private static final byte[] BAD_SIGNATURE = new byte[] {5, 7};

    /**
     * Process a file.
     *
     * @param file the file to process
     * @param mode the mode
     * @throws IOException
     */
    static void processFile(final File file, final PatchingTaskContext.Mode mode) throws IOException {
        if (mode == PatchingTaskContext.Mode.APPLY) {
            modifyEndOfCentralDirSig(file, INVALIDATION_VALUE, GOOD_SIGNATURE[0], GOOD_SIGNATURE[1]);
        } else if (mode == PatchingTaskContext.Mode.ROLLBACK) {
            modifyEndOfCentralDirSig(file, RESTORE_VALUE, BAD_SIGNATURE[0], BAD_SIGNATURE[1]);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Modify the end of the central directory record of a jar file. The end of central directory record value
     * is little-endian 0x06054b50. The file can be crippled by setting it to 0x07054b50
     *
     * @throws java.io.IOException
     */
    static void modifyEndOfCentralDirSig(final File file, final byte val, final int sig1, final int sig2) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            long filePos = raf.length();
            boolean keepSeeking = true;
            int readVal = 0;
            int prevReadVal = 0;
            while (keepSeeking) {
                filePos--;
                raf.seek(filePos);
                prevReadVal = readVal;
                readVal = raf.read();
                if (readVal == sig1 && prevReadVal == sig2) {
                    raf.write(val);
                    keepSeeking = false;
                }
            }
            raf.close();
        } finally {
            IoUtils.safeClose(raf);
        }
    }

}
