/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.impl;

import java.io.OutputStream;
import java.io.PrintStream;

public class TelnetPrintStream extends PrintStream {

    private final byte[] CRLF = new byte[]{(byte) '\r', (byte) '\n'};

    public TelnetPrintStream(OutputStream out) {
        super(out);
    }

    public void println() {
        newLine();
    }

    public void println(String x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(long x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(char x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(boolean x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(float x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(double x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(int x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(char[] x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    private void newLine() {
        try {
            this.write(CRLF);
        } catch (Exception e) {
        }
    }
}
