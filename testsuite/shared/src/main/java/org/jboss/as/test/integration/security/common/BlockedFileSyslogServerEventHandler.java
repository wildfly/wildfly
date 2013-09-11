package org.jboss.as.test.integration.security.common;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.event.printstream.FileSyslogServerEventHandler;

public class BlockedFileSyslogServerEventHandler extends FileSyslogServerEventHandler {

    private static final long serialVersionUID = -3814601581286016000L;
    private BlockingQueue<String> queue;

    public BlockedFileSyslogServerEventHandler(BlockingQueue<String> queue, String fileName, boolean append) throws IOException {
        super(fileName, append);
        this.queue = queue;
    }

    public void event(SyslogServerIF syslogServer, SyslogServerEventIF event) {
        super.event(syslogServer, event);
        queue.offer(new String(""));
    }

}
