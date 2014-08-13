/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.services.resourceadapters.deployment;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.Locale;

/**
 * JBossLogPrintWriter PrintWriter for JBossLogging
 *
 * @author <a href="mailto:jizhang@redhat.com">Jeff Zhang</a>
 */
public final class JBossLogPrintWriter extends PrintWriter {

    private final String deploymentName;
    private final BasicLogger logger;
    private final Logger.Level level = Logger.Level.INFO;

    private StringBuilder buffer = new StringBuilder();
    private Formatter formatter;

    public JBossLogPrintWriter(String deploymentName, BasicLogger logger) {
        super(new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                // do nothing
            }

            @Override
            public void write(final byte[] b) throws IOException {
                // do nothing
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                // do nothing
            }
        });
        this.deploymentName = deploymentName;
        this.logger = logger;
    }


    @Override
    public void write(int c) {
        synchronized (lock) {
            if (c == '\n') {
                outputLogger();
            } else {
                buffer.append((char) c);
            }
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        synchronized (lock) {
            int mark = 0;
            int i;
            for (i = 0; i < len; i++) {
                final char c = cbuf[off + i];
                if (c == '\n') {
                    buffer.append(cbuf, mark + off, i - mark);
                    outputLogger();
                    mark = i + 1;
                }
            }
            buffer.append(cbuf, mark + off, i - mark);
        }
    }

    @Override
    public void write(char[] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(String str, int off, int len) {
        synchronized (lock) {
            int mark = 0;
            int i;
            for (i = 0; i < len; i++) {
                final char c = str.charAt(off + i);
                if (c == '\n') {
                    buffer.append(str.substring(mark + off, off + i));
                    outputLogger();
                    mark = i + 1;
                }
            }
            buffer.append(str.substring(mark + off, off + i));
        }
    }

    @Override
    public void write(String s) {
        write(s, 0, s.length());
    }

    private void outputLogger() {
        if (buffer.length() > 0) {
            logger.log(level, deploymentName + ": " + buffer.toString());
            buffer.setLength(0);
        }
    }

    @Override
    public void flush() {
        synchronized (lock) {
            outputLogger();
        }
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public boolean checkError() {
        flush();
        return false;
    }

    @Override
    protected void setError() {
    }

    @Override
    protected void clearError() {
    }

    @Override
    public void print(boolean b) {
        write(String.valueOf(b) );
    }

    @Override
    public void print(char c) {
        write(c);
    }

    @Override
    public void print(int i) {
        write(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        write(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        write(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        write(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        write(s);
    }

    @Override
    public void print(String s) {
        if (s != null) {
            write(s);
        }
    }

    @Override
    public void print(Object obj) {
        if (obj != null) {
            write(String.valueOf(obj));
        }
    }

    private void newLine() {
        outputLogger();
    }

    @Override
    public void println() {
        newLine();
    }

    @Override
    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(char x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(int x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(long x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(float x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(double x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(char[] x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(String x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    @Override
    public void println(Object x) {
        String s = String.valueOf(x);
        synchronized (lock) {
            print(s);
            println();
        }
    }

    @Override
    public JBossLogPrintWriter printf(String format, Object ... args) {
        return format(format, args);
    }

    @Override
    public JBossLogPrintWriter printf(Locale l, String format, Object ... args) {
        return format(l, format, args);
    }

    public JBossLogPrintWriter format(String format, Object ... args) {
        synchronized (lock) {
            if ((formatter == null)
                || (formatter.locale() != Locale.getDefault()))
                formatter = new Formatter(this);
            formatter.format(Locale.getDefault(), format, args);
        }
        return this;
    }

    public JBossLogPrintWriter format(Locale l, String format, Object ... args) {
        synchronized (lock) {
            if ((formatter == null) || (formatter.locale() != l))
                formatter = new Formatter(this, l);
            formatter.format(l, format, args);
        }
        return this;
    }

    @Override
    public JBossLogPrintWriter append(CharSequence csq) {
        if (csq != null)
            write(csq.toString());
        return this;
    }

    @Override
    public JBossLogPrintWriter append(CharSequence csq, int start, int end) {
        if (csq != null)
            write(csq.subSequence(start, end).toString());
        return this;
    }

    @Override
    public JBossLogPrintWriter append(char c) {
        write(c);
        return this;
    }
}
