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

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import org.jboss.as.patching.PatchLogger;

/**
 * Cripple a JAR or other zip file by flipping a bit in the end of central directory record
 * This process can be reversed by flipping the bit back. Useful for rendering JARs non-executable.
 *
 * based on the {@linkplain org.jboss.as.server.deployment.scanner.ZipCompletionScanner}
 *
 * @author David Jorm
 * @author Emanuel Muckenhuber
 */
class PatchModuleInvalidationUtils {

    /**
     * Local file header marker
     */
    public static final long LOCSIG = 0x04034b50L;
    /**
     * Extra data descriptor marker
     */
    public static final long EXTSIG = 0x08074b50L;
    /**
     * Central directory file header marker
     */
    public static final long CENSIG = 0x02014b50L;

    /**
     * End of central directory record marker
     */
    public static final int GOOD_ENDSIG = 0x06054b50; // Good signature
    public static final int CRIPPLED_ENDSIG = 0x07054b50; // Crippled signature

    /**
     * Length of the fixed portion of a local file header
     */
    public static final int LOCLEN = 30;
    /**
     * Length of the fixed portion of a central directory file header
     */
    public static final int CENLEN = 46;
    /**
     * Length of the fixed portion of a End of central directory record
     */
    public static final int ENDLEN = 22;

    /**
     * Position of the filename length in a local file header
     */
    public static final int LOC_FILENAMELEN = 26;
    /**
     * Position of the extra field length in a local file header
     */
    public static final int LOC_EXTFLDLEN = 28;

    /**
     * Position of the associated local file's compressed size in the central directory file header
     */
    public static final int CENSIZ = 20;
    /**
     * Position of the associated local file's offset in the central directory file header
     */
    public static final int CEN_LOC_OFFSET = 32;

    /**
     * Position of the 'start of central directory' field in a end of central directory record
     */
    public static final int END_CENSTART = 16;
    /**
     * END_CENSTART value that indicates the zip is in ZIP 64 format
     */
    public static final long ZIP64_MARKER = 0xFFFFFFFFL;
    /**
     * Position of the comment length in a end of central directory record
     */
    public static final int END_COMMENTLEN = 20;

    private static final int MAX_REVERSE_SCAN = (1 << 16) + ENDLEN;
    private static final int CHUNK_SIZE = 4096;
    private static final int ALPHABET_SIZE = 256;

    private static final byte[] GOOD_ENDSIG_PATTERN = new byte[]{0x06, 0x05, 0x4b, 0x50}; // good signature
    private static final byte[] CRIPPLED_ENDSIG_PATTERN = new byte[]{0x07, 0x05, 0x4b, 0x50}; // crippled signature
    private static final int SIG_PATTERN_LENGTH = 4;

    private static final int[] GOOD_END_BAD_BYTE_SKIP = new int[ALPHABET_SIZE];
    private static final int[] CRIPPLED_BAD_BYTE_SKIP = new int[ALPHABET_SIZE];

    private static final byte[] LOCSIG_PATTERN = new byte[]{0x50, 0x4b, 0x03, 0x04};
    private static final int[] LOC_BAD_BYTE_SKIP = new int[ALPHABET_SIZE];

    static {
        // Set up the Boyer Moore "bad character arrays" for our 3 patterns
        computeBadByteSkipArray(GOOD_ENDSIG_PATTERN, GOOD_END_BAD_BYTE_SKIP);
        computeBadByteSkipArray(CRIPPLED_ENDSIG_PATTERN, CRIPPLED_BAD_BYTE_SKIP);
        computeBadByteSkipArray(LOCSIG_PATTERN, LOC_BAD_BYTE_SKIP);
    }

    /**
     * Prevent instantiation
     */
    private PatchModuleInvalidationUtils() {
    }

    /**
     * Process a file.
     *
     * @param file the file to be processed
     * @param mode the patching mode
     * @throws IOException
     */
    static void processFile(final File file, final PatchingTaskContext.Mode mode) throws IOException {
        if (mode == PatchingTaskContext.Mode.APPLY) {
            updateJar(file, GOOD_ENDSIG_PATTERN, GOOD_END_BAD_BYTE_SKIP, CRIPPLED_ENDSIG, GOOD_ENDSIG);
        } else if (mode == PatchingTaskContext.Mode.ROLLBACK) {
            updateJar(file, CRIPPLED_ENDSIG_PATTERN, CRIPPLED_BAD_BYTE_SKIP, GOOD_ENDSIG, CRIPPLED_ENDSIG);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Update the central directory signature of a .jar.
     *
     * @param file          the file to process
     * @param searchPattern the search patter to use
     * @param badSkipBytes  the bad bytes skip table
     * @param newSig        the new signature
     * @param endSig        the expected signature
     * @throws IOException
     */
    private static void updateJar(final File file, final byte[] searchPattern, final int[] badSkipBytes, final int newSig, final int endSig) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            final FileChannel channel = raf.getChannel();
            try {
                long pos = channel.size() - ENDLEN;
                if (!validateEndRecord(file, channel, pos, endSig)) {
                    pos = scanForEndSig(file, channel, searchPattern, badSkipBytes, endSig);
                }
                if (pos == -1) {
                    // Don't fail patching if we cannot validate a valid zip
                    PatchLogger.ROOT_LOGGER.cannotInvalidateZip(file.getAbsolutePath());
                    return;
                }
                // Update the central directory record
                channel.position(pos);
                final ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putInt(newSig);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            } finally {
                safeClose(channel);
            }
        } finally {
            safeClose(raf);
        }
    }

    /**
     * Validates that the data structure at position startEndRecord has a field in the expected position
     * that points to the start of the first central directory file, and, if so, that the file
     * has a complete end of central directory record comment at the end.
     *
     * @param file           the file being checked
     * @param channel        the channel
     * @param startEndRecord the start of the end of central directory record
     * @param endSig         the end of central dir signature
     * @return true if it can be confirmed that the end of directory record points to a central directory
     *         file and a complete comment is present, false otherwise
     * @throws java.io.IOException
     */
    private static boolean validateEndRecord(File file, FileChannel channel, long startEndRecord, long endSig) throws IOException {

        try {
            channel.position(startEndRecord);

            final ByteBuffer endDirHeader = getByteBuffer(ENDLEN);
            read(endDirHeader, channel);
            if (endDirHeader.limit() < ENDLEN) {
                // Couldn't read the full end of central directory record header
                return false;
            } else if (getUnsignedInt(endDirHeader, 0) != endSig) {
                return false;
            }

            long pos = getUnsignedInt(endDirHeader, END_CENSTART);
            // TODO deal with Zip64
            if (pos == ZIP64_MARKER) {
                return false;
            }

            ByteBuffer cdfhBuffer = getByteBuffer(CENLEN);
            read(cdfhBuffer, channel, pos);
            long header = getUnsignedInt(cdfhBuffer, 0);
            if (header == CENSIG) {
                long firstLoc = getUnsignedInt(cdfhBuffer, CEN_LOC_OFFSET);
                long firstSize = getUnsignedInt(cdfhBuffer, CENSIZ);
                if (firstLoc == 0) {
                    // normal case -- first bytes are the first local file
                    if (!validateLocalFileRecord(channel, 0, firstSize)) {
                        return false;
                    }
                } else {
                    // confirm that firstLoc is indeed the first local file
                    long fileFirstLoc = scanForLocSig(channel);
                    if (firstLoc != fileFirstLoc) {
                        if (fileFirstLoc == 0) {
                            return false;
                        } else {
                            // scanForLocSig() found a LOCSIG, but not at position zero and not
                            // at the expected position.
                            // With a file like this, we can't tell if we're in a nested zip
                            // or we're in an outer zip and had the bad luck to find random bytes
                            // that look like LOCSIG.
                            return false;
                        }
                    }
                }

                // At this point, endDirHeader points to the correct end of central dir record.
                // Just need to validate the record is complete, including any comment
                int commentLen = getUnsignedShort(endDirHeader, END_COMMENTLEN);
                long commentEnd = startEndRecord + ENDLEN + commentLen;
                return commentEnd <= channel.size();
            }

            return false;
        } catch (EOFException eof) {
            // pos or firstLoc weren't really positions and moved us to an invalid location
            return false;
        }
    }

    /**
     * Boyer Moore scan that proceeds backwards from the end of the file looking for endsig
     *
     * @param file     the file being checked
     * @param channel  the channel
     * @param pattern  the search pattern
     * @param byteSkip the bad bytes skip table
     * @param endSig   the end of central dir signature
     * @return
     * @throws IOException
     */
    private static long scanForEndSig(final File file, final FileChannel channel, final byte[] pattern, final int[] byteSkip, final long endSig) throws IOException {

        // TODO Consider just reading in MAX_REVERSE_SCAN bytes -- increased peak memory cost but less complex

        ByteBuffer bb = getByteBuffer(CHUNK_SIZE);
        long start = channel.size();
        long end = Math.max(0, start - MAX_REVERSE_SCAN);
        long channelPos = Math.max(0, start - CHUNK_SIZE);
        long lastChannelPos = channelPos;
        while (lastChannelPos >= end) {

            read(bb, channel, channelPos);

            int actualRead = bb.limit();
            int bufferPos = actualRead - 1;
            while (bufferPos >= SIG_PATTERN_LENGTH) {

                // Following is based on the Boyer Moore algorithm but simplified to reflect
                // a) the pattern is static
                // b) the pattern has no repeating bytes

                int patternPos;
                for (patternPos = SIG_PATTERN_LENGTH - 1;
                     patternPos >= 0 && pattern[patternPos] == bb.get(bufferPos - patternPos);
                     --patternPos) {
                    // empty loop while bytes match
                }

                // Switch gives same results as checking the "good suffix array" in the Boyer Moore algorithm
                switch (patternPos) {
                    case -1: {
                        // Pattern matched. Confirm is this is the start of a valid end of central dir record
                        long startEndRecord = channelPos + bufferPos - SIG_PATTERN_LENGTH + 1;
                        if (validateEndRecord(file, channel, startEndRecord, endSig)) {
                            return startEndRecord;
                        }
                        // wasn't a valid end record; continue scan
                        bufferPos -= 4;
                        break;
                    }
                    case 3: {
                        // No bytes matched; the common case.
                        // With our pattern, this is the only case where the Boyer Moore algorithm's "bad char array" may
                        // produce a shift greater than the "good suffix array" (which would shift 1 byte)
                        int idx = bb.get(bufferPos - patternPos) - Byte.MIN_VALUE;
                        bufferPos -= byteSkip[idx];
                        break;
                    }
                    default:
                        // 1 or more bytes matched
                        bufferPos -= 4;
                }
            }

            // Move back a full chunk. If we didn't read a full chunk, that's ok,
            // it means we read all data and the outer while loop will terminate
            if (channelPos <= bufferPos) {
                break;
            }
            lastChannelPos = channelPos;
            channelPos -= Math.min(channelPos - bufferPos, CHUNK_SIZE - bufferPos);
        }

        return -1;
    }

    /**
     * Boyer Moore scan that proceeds forwards from the end of the file looking for the first LOCSIG
     */
    private static long scanForLocSig(FileChannel channel) throws IOException {

        channel.position(0);

        ByteBuffer bb = getByteBuffer(CHUNK_SIZE);
        long end = channel.size();
        while (channel.position() <= end) {

            read(bb, channel);

            int bufferPos = 0;
            while (bufferPos <= bb.limit() - SIG_PATTERN_LENGTH) {

                // Following is based on the Boyer Moore algorithm but simplified to reflect
                // a) the size of the pattern is static
                // b) the pattern is static and has no repeating bytes

                int patternPos;
                for (patternPos = SIG_PATTERN_LENGTH - 1;
                     patternPos >= 0 && LOCSIG_PATTERN[patternPos] == bb.get(bufferPos + patternPos);
                     --patternPos) {
                    // empty loop while bytes match
                }

                // Outer switch gives same results as checking the "good suffix array" in the Boyer Moore algorithm
                switch (patternPos) {
                    case -1: {
                        // Pattern matched. Confirm is this is the start of a valid local file record
                        long startLocRecord = channel.position() - bb.limit() + bufferPos;
                        long currentPos = channel.position();
                        if (validateLocalFileRecord(channel, startLocRecord, -1)) {
                            return startLocRecord;
                        }
                        // Restore position in case it shifted
                        channel.position(currentPos);

                        // wasn't a valid local file record; continue scan
                        bufferPos += 4;
                        break;
                    }
                    case 3: {
                        // No bytes matched; the common case.
                        // With our pattern, this is the only case where the Boyer Moore algorithm's "bad char array" may
                        // produce a shift greater than the "good suffix array" (which would shift 1 byte)
                        int idx = bb.get(bufferPos + patternPos) - Byte.MIN_VALUE;
                        bufferPos += LOC_BAD_BYTE_SKIP[idx];
                        break;
                    }
                    default:
                        // 1 or more bytes matched
                        bufferPos += 4;
                }
            }
        }

        return -1;
    }

    /**
     * Checks that the data starting at startLocRecord looks like a local file record header.
     *
     * @param channel        the channel
     * @param startLocRecord offset into channel of the start of the local record
     * @param compressedSize expected compressed size of the file, or -1 to indicate this isn't known
     */
    private static boolean validateLocalFileRecord(FileChannel channel, long startLocRecord, long compressedSize) throws IOException {

        ByteBuffer lfhBuffer = getByteBuffer(LOCLEN);
        read(lfhBuffer, channel, startLocRecord);
        if (lfhBuffer.limit() < LOCLEN || getUnsignedInt(lfhBuffer, 0) != LOCSIG) {
            return false;
        }

        if (compressedSize == -1) {
            // We can't further evaluate
            return true;
        }

        int fnLen = getUnsignedShort(lfhBuffer, LOC_FILENAMELEN);
        int extFieldLen = getUnsignedShort(lfhBuffer, LOC_EXTFLDLEN);
        long nextSigPos = startLocRecord + LOCLEN + compressedSize + fnLen + extFieldLen;

        read(lfhBuffer, channel, nextSigPos);
        long header = getUnsignedInt(lfhBuffer, 0);
        return header == LOCSIG || header == EXTSIG || header == CENSIG;
    }

    private static ByteBuffer getByteBuffer(int capacity) {
        ByteBuffer b = ByteBuffer.allocate(capacity);
        b.order(ByteOrder.LITTLE_ENDIAN);
        return b;
    }

    private static void read(ByteBuffer bb, FileChannel ch) throws IOException {
        bb.clear();
        ch.read(bb);
        bb.flip();
    }

    private static void read(ByteBuffer bb, FileChannel ch, long pos) throws IOException {
        bb.clear();
        ch.read(bb, pos);
        bb.flip();
    }

    private static long getUnsignedInt(ByteBuffer bb, int offset) {
        return (bb.getInt(offset) & 0xffffffffL);
    }

    private static int getUnsignedShort(ByteBuffer bb, int offset) {
        return (bb.getShort(offset) & 0xffff);
    }

    /**
     * Fills the Boyer Moore "bad character array" for the given pattern
     */
    private static void computeBadByteSkipArray(byte[] pattern, int[] badByteArray) {
        for (int a = 0; a < ALPHABET_SIZE; a++) {
            badByteArray[a] = pattern.length;
        }

        for (int j = 0; j < pattern.length - 1; j++) {
            badByteArray[pattern[j] - Byte.MIN_VALUE] = pattern.length - j - 1;
        }
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

}