package org.jboss.as.test.integration.ejb.access.log.util;

import org.jboss.as.test.shared.TimeoutUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Class to capture server console output
 */
public class ServerStdout extends OutputStream {
    private static final int DFT_TIMEOUT = 1000;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final String[] EMPTY = new String[0];

    private final OutputStream dftStdout;
    private byte[] buffer;
    private int bufferLen;
    private String[] lines;
    private int lineLen;
    private int offset = 0;

    public ServerStdout(final OutputStream dftStdout) {
        this.dftStdout = dftStdout;
        buffer = new byte[1024];
        lines = new String[100];
    }

    @Override
    public synchronized void write(final int b) throws IOException {
        append(b);
        dftStdout.write(b);
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        // Check the array of a new line
        for (int i = off; i < len; i++) {
            append(b[i]);
        }
        dftStdout.write(b, off, len);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
        dftStdout.write(b);
    }

    @Override
    public void flush() throws IOException {
        dftStdout.flush();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        final Iterator<String> iter = Arrays.asList(getLines()).iterator();
        while (iter.hasNext()) {
            result.append(iter.next());
            if (iter.hasNext()) {
                result.append(System.lineSeparator());
            }
        }
        return result.toString();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void append(final int b) {
        if (b == '\n') {
            ensureLineCapacity(lineLen + 1);
            lines[lineLen++] = new String(buffer, 0, bufferLen, StandardCharsets.UTF_8);
            bufferLen = 0;
        } else if (b == '\r') {
            // For out purposes just ignore this character
        } else {
            ensureBufferCapacity(bufferLen + 1);
            buffer[bufferLen++] = (byte) b;
        }
    }

    private void ensureBufferCapacity(final int minCapacity) {
        if (minCapacity - buffer.length > 0)
            growBuffer(minCapacity);
    }

    private void growBuffer(final int minCapacity) {
        final int oldCapacity = buffer.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
        }
        buffer = Arrays.copyOf(buffer, newCapacity);
    }

    private void ensureLineCapacity(final int minCapacity) {
        if (minCapacity - lines.length > 0)
            growLine(minCapacity);
    }

    private void growLine(final int minCapacity) {
        final int oldCapacity = lines.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
        }
        lines = Arrays.copyOf(lines, newCapacity);
    }

    synchronized String[] getLines() {
        if (lineLen == 0) {
            return EMPTY;
        }
        return Arrays.copyOf(lines, lineLen);
    }

    /**
     * return lines from "offset" to the end of the "lines" array
     */
    synchronized String[] getLines(final int offset) {
        if (lineLen == 0) {
            return EMPTY;
        }
        return Arrays.copyOfRange(lines, offset, lineLen);
    }

    public synchronized String[] getNewLinesSync() {
        if (lines.length > this.offset) {
            final String[] retval = getLines(this.offset);
            if (retval.length > 0) {
                this.offset = retval.length;
                return retval;
            }
        }
        return null;
    }

    /**
     * @return lines added after the last read which actually returned a not null value (start picking lines at "this.offset");
     */
    public String[] getNewLines() {
        int timeout = TimeoutUtil.adjust(DFT_TIMEOUT) * 1000;
        final long sleep = 100L;
        while (timeout > 0) {
            long before = System.currentTimeMillis();

            String[] newLines = getNewLinesSync();
            if (newLines != null && newLines.length > 0) {
                return newLines;
            }

            timeout -= (System.currentTimeMillis() - before);
            try {
                TimeUnit.MILLISECONDS.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timeout -= sleep;
        }
        return null;
    }
}
