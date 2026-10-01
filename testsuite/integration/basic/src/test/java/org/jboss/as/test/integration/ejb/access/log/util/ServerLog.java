package org.jboss.as.test.integration.ejb.access.log.util;

import org.jboss.as.test.shared.TimeoutUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Incrementally returns added lines
 */
public class ServerLog {
    private static final int DFT_TIMEOUT = 1000;
    private RandomAccessFile pointer;

    public ServerLog(Path file, long offset) throws IOException {
        pointer = new RandomAccessFile(file.toFile(), "r");
        pointer.seek(offset);
    }

    /**
     * Incrementally returns lines added since the last read.
     *
     * <p>Waits up to the timeout for the first line to appear, then keeps reading
     * for one extra poll interval so that async log writes (e.g. from AsyncEventLogger)
     * that arrive slightly after the triggering log line are also included.
     *
     * @return lines added since last read, or null if none arrived within the timeout
     */
    public String[] getNewLines() throws IOException, InterruptedException {
        List<String> lines = new ArrayList<>();
        int timeout = TimeoutUtil.adjust(DFT_TIMEOUT) * 1000;
        final long sleep = 100L;
        // Phase 1: wait until at least one line appears.
        while (timeout > 0 && lines.isEmpty()) {
            long before = System.currentTimeMillis();
            String line;
            while ((line = pointer.readLine()) != null) {
                lines.add(line);
            }
            if (!lines.isEmpty()) break;
            timeout -= (System.currentTimeMillis() - before);
            TimeUnit.MILLISECONDS.sleep(sleep);
            timeout -= sleep;
        }
        // Phase 2: keep draining for one more interval to catch async writes.
        if (!lines.isEmpty()) {
            TimeUnit.MILLISECONDS.sleep(sleep);
            String line;
            while ((line = pointer.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines.isEmpty() ? null : lines.toArray(new String[0]);
    }

    public long getOffset() throws IOException {
        return pointer.getFilePointer();
    }
}
