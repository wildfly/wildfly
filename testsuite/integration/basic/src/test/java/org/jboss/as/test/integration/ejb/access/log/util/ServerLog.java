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
     * Incrementally returns added lines
     * @return lines added since last read
     * @throws IOException
     */
    public String[] getNewLines() throws IOException, InterruptedException {
        System.out.println("# offset BEFORE " + pointer.getFilePointer());
        List<String> lines = new ArrayList<>();
        int timeout = TimeoutUtil.adjust(DFT_TIMEOUT) * 1000;
        final long sleep = 100L;
        while (timeout > 0) {
            long before = System.currentTimeMillis();
            String line;
            while ((line = pointer.readLine()) != null) {
                lines.add(line);
            }
            if (lines != null) break;
            timeout -= (System.currentTimeMillis() - before);
            System.out.println("# offset TIMEOUT " + timeout);
            TimeUnit.MILLISECONDS.sleep(sleep);
            timeout -= sleep;
        }
        System.out.println("# offset AFTER " + pointer.getFilePointer());
        return lines.size() > 0 ? lines.toArray(new String[lines.size()]) : null;
    }

    public long getOffset() throws IOException {
        return pointer.getFilePointer();
    }
}
