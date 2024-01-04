/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.naming.java.permission;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jboss.as.naming.logging.NamingLogger;
import org.wildfly.common.iteration.CodePointIterator;

final class JndiPermissionNameParser {
    private JndiPermissionNameParser() {
    }

    static Iterator<String> nameIterator(final String string) {
        return new ParsingIterator(string);
    }

    static Iterator<String> segmentsIterator(String[] segments) {
        return new SegmentsIterator(segments);
    }

    static String[] toArray(Iterator<String> iter) {
        return toArray(iter, 0);
    }

    private static String[] toArray(final Iterator<String> iter, final int size) {
        if (iter.hasNext()) {
            String next = iter.next();
            String[] array = toArray(iter, size + 1);
            array[size] = next;
            return array;
        } else {
            return new String[size];
        }
    }

    static class SegmentsIterator implements Iterator<String> {
        private final String[] segments;
        private int idx;

        SegmentsIterator(final String[] segments) {
            this.segments = segments;
        }

        public boolean hasNext() {
            return idx < segments.length;
        }

        public String next() {
            return segments[idx ++];
        }

        String[] getSegments() {
            return segments;
        }
    }

    static class ParsingIterator implements Iterator<String> {
        private final CodePointIterator cpi;
        private final StringBuilder b;
        private final String string;
        private boolean hasNext = true;

        ParsingIterator(final String string) {
            this.string = string;
            cpi = CodePointIterator.ofString(string);
            b = new StringBuilder();
        }

        public boolean hasNext() {
            return hasNext;
        }

        public String next() {
            if (! hasNext()) throw new NoSuchElementException();
            final StringBuilder b = this.b;
            final CodePointIterator cpi = this.cpi;
            int cp;
            while (cpi.hasNext()) {
                cp = cpi.next();
                if (cp == '\\') {
                    // skip the next code point always
                    if (! cpi.hasNext()) {
                        throw NamingLogger.ROOT_LOGGER.invalidJndiName(string);
                    }
                    b.appendCodePoint(cpi.next());
                } else if (cp == '"' || cp == '\'') {
                    int q = cp;
                    if (! cpi.hasNext()) {
                        throw NamingLogger.ROOT_LOGGER.invalidJndiName(string);
                    }
                    for (;;) {
                        cp = cpi.next();
                        if (cp == '\\') {
                            // skip the next code point always
                            if (! cpi.hasNext()) {
                                throw NamingLogger.ROOT_LOGGER.invalidJndiName(string);
                            }
                            b.appendCodePoint(cpi.next());
                        } else if (cp == q) {
                            break;
                        } else {
                            b.appendCodePoint(cp);
                        }
                        if (! cpi.hasNext()) {
                            throw NamingLogger.ROOT_LOGGER.invalidJndiName(string);
                        }
                    }
                } else if (cp == '/') {
                    final String s = b.toString();
                    b.setLength(0);
                    return s;
                } else {
                    b.appendCodePoint(cp);
                }
            }
            final String s = b.toString();
            b.setLength(0);
            hasNext = false;
            return s;
        }
    }
}
