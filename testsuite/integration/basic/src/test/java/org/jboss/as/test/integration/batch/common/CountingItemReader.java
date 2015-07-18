/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.common;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class CountingItemReader implements ItemReader {

    @Inject
    @BatchProperty(name = "reader.start")
    private int start;

    @Inject
    @BatchProperty(name = "reader.end")
    private int end;

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (end == 0) {
            end = 10;
        }
        counter.set(start);
    }

    @Override
    public void close() throws Exception {
        counter.set(0);
    }

    @Override
    public Object readItem() throws Exception {
        final int result = counter.incrementAndGet();
        if (result > end) {
            return null;
        }
        return result;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return counter.get();
    }
}
