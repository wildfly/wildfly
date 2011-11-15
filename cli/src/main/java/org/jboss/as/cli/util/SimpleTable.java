/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleTable {

    private final Object[] header;
    private final int[] columnLengths;
    private final List<String[]> lines = new ArrayList<String[]>();

    public SimpleTable(String[] header) {
        if(header == null || header.length == 0) {
            throw new IllegalArgumentException("header can't be null or empty.");
        }
        this.header = new String[header.length];
        columnLengths = new int[header.length];
        for(int i = 0; i < header.length; ++i) {
            final String name = header[i];
            if(name == null) {
                throw new IllegalArgumentException("One of the headers is null: " + Arrays.asList(header));
            }
            this.header[i] = name;
            columnLengths[i] = name.length() + 1;
        }
    }

    public void addLine(String[] line) {
        if(line == null) {
           throw new IllegalArgumentException("The line can't be null.");
        }
        if(line.length != header.length) {
            throw new IllegalArgumentException("Line length " + line.length + " doesn't match headers' length " + header.length);
        }

        final String[] values = new String[line.length];
        for(int i = 0; i < line.length; ++i) {
            String value = line[i];
            if(value == null) {
                value = "null";
            }
            values[i] = value;
            if(columnLengths[i] < value.length() + 1) {
                columnLengths[i] = value.length() + 1;
            }
        }
        lines.add(values);
    }

    public int size() {
        return lines.size();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        Formatter formatter = new Formatter(buf);
        final StringBuilder formatBuf = new StringBuilder();
        for(int length : columnLengths) {
            formatBuf.append("%-").append(length).append('s');
        }
        final String format = formatBuf.toString();
        formatter.format(format, header);
        for(Object[] line : lines) {
            buf.append('\n');
            formatter.format(format, line);
        }
        return buf.toString();
    }
}
