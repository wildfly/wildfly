/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.process;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings({ "SynchronizeOnNonFinalField" })
final class SynchronizedWriter extends Writer {
    private final Writer delegate;

    SynchronizedWriter(final PrintStream target) {
        super(target);
        delegate = new OutputStreamWriter(target);
    }

    public void write(final int c) throws IOException {
        synchronized (lock) {
            delegate.write(c);
        }
    }

    public void write(final char[] cbuf) throws IOException {
        synchronized (lock) {
            delegate.write(cbuf);
        }
    }

    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        synchronized (lock) {
            delegate.write(cbuf, off, len);
        }
    }

    public void write(final String str) throws IOException {
        synchronized (lock) {
            delegate.write(str);
        }
    }

    public void write(final String str, final int off, final int len) throws IOException {
        synchronized (lock) {
            delegate.write(str, off, len);
        }
    }

    public Writer append(final CharSequence csq) throws IOException {
        synchronized (lock) {
            return delegate.append(csq);
        }
    }

    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        synchronized (lock) {
            return delegate.append(csq, start, end);
        }
    }

    public Writer append(final char c) throws IOException {
        synchronized (lock) {
            return delegate.append(c);
        }
    }

    public void flush() throws IOException {
        synchronized (lock) {
            delegate.flush();
        }
    }

    public void close() throws IOException {
        // nada
    }
}
