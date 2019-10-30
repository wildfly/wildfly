/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
