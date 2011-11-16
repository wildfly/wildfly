/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A stream which ignores EOF and polls for may data.
 *
 * @author Jason T. Greene
 */
public class PollingFileInputStream extends FilterInputStream {
    private AtomicBoolean done;

    PollingFileInputStream(InputStream in, AtomicBoolean done) {
        super(in);
        this.done = done;
    }

    @Override
    public int read() throws IOException {
        int i = in.read();
        while (i == -1 && !done.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            i = in.read();
        }

        return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
       int i = in.read(b, off, len);
        while (i == -1 && !done.get()) {
               try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            i = in.read(b, off, len);
        }

        return i;
    }
}
