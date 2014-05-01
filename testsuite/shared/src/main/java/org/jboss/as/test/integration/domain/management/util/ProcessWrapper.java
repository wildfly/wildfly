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

package org.jboss.as.test.integration.domain.management.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import org.jboss.as.process.protocol.StreamUtils;

/**
 * Basic process test wrapper.
 *
 * @author Emanuel Muckenhuber
 */
class ProcessWrapper {

    private final String processName;
    private final List<String> command;
    private final Map<String, String> env;
    private final String workingDirectory;

    private Process process;
    private volatile boolean stopped;

    ProcessWrapper(final String processName, final List<String> command, final Map<String, String> env,
            final String workingDirectory) {
        assert processName != null;
        assert command != null;
        assert env != null;
        assert workingDirectory != null;
        this.processName = processName;
        this.command = command;
        this.env = env;
        this.workingDirectory = workingDirectory;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                ProcessWrapper.this.stop();
            }
        });
    }

    int getExitValue() {
        synchronized (this) {
            try {
                return process.exitValue();
            } catch (IllegalThreadStateException e) {
                return -1;
            }
        }
    }

    void start() throws Exception {
        synchronized (this) {
            if (stopped) {
                throw new IllegalStateException();
            }
            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.environment().putAll(env);
            builder.directory(new File(workingDirectory));
            process = builder.start();

            final InputStream stdout = process.getInputStream();
            final Runnable consoleConsumer = new ConsoleConsumer(stdout, System.out);
            new Thread(consoleConsumer, "Console consumer " + processName).start();
        }
    }

    int waitFor() throws InterruptedException {
        final Process process;
        synchronized (this) {
            process = this.process;
        }
        if (process != null) {
            return process.waitFor();
        }
        return 0;
    }

    void stop() {
        synchronized (this) {
            boolean stopped = this.stopped;
            if (!stopped) {
                this.stopped = true;
                final Process process = this.process;
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "ProcessWrapper [processName=" + processName + ", command=" + command + ", env=" + env + ", workingDirectory="
                + workingDirectory + ", stopped=" + stopped + "]";
    }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     *
     * @author Stuart Douglas
     *
     */
    private class ConsoleConsumer implements Runnable {
        private final InputStream source;
        private final PrintStream target;
        private final boolean writeOutput = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                // this needs a better name
                String val = System.getProperty("org.jboss.as.writeconsole");
                return val == null || !"false".equals(val);
            }
        });

        private ConsoleConsumer(final InputStream source, final PrintStream target) {
            this.source = source;
            this.target = target;
        }

        public void run() {
            final InputStream source = this.source;
            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = source.read(buf)) != -1) {
                    if (writeOutput)
                        System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
                if (!ProcessWrapper.this.stopped) {
                    e.printStackTrace(target);
                }
            } finally {
                StreamUtils.safeClose(source);
            }
        }
    }

}
