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

package org.jboss.as.test.shared;

import java.io.File;
import java.util.Properties;

/**
 * Parses a string and replaces any references to system properties or environment variables in the string
 *
 * @author Jaikiran Pai (copied from JBoss DMR project)
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PropertiesValueResolver {

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;
    private static final int RESOLVED = 3;
    private static final int DEFAULT = 4;

    /**
     * Replace properties of the form:
     * <code>${<i>&lt;[env.]name&gt;[</i>,<i>&lt;[env.]name2&gt;[</i>,<i>&lt;[env.]name3&gt;...]][</i>:<i>&lt;default&gt;]</i>}</code>
     *
     * @param value - either a system property or environment variable reference
     * @return the value of the system property or environment variable referenced if
     *         it exists
     */
    public static String replaceProperties(final String value) {
        return replaceProperties(value, System.getProperties());
    }
    /**
     * Replace properties of the form:
     * <code>${<i>&lt;[env.]name&gt;[</i>,<i>&lt;[env.]name2&gt;[</i>,<i>&lt;[env.]name3&gt;...]][</i>:<i>&lt;default&gt;]</i>}</code>
     *
     * @param value - either a system property or environment variable reference
     * @return the value of the system property or environment variable referenced if
     *         it exists
     */
    public static String replaceProperties(final String value, final Properties properties) {
        final StringBuilder builder = new StringBuilder();
        final int len = value.length();
        int state = INITIAL;
        int start = -1;
        int nameStart = -1;
        String resolvedValue = null;
        for (int i = 0; i < len; i = value.offsetByCodePoints(i, 1)) {
            final int ch = value.codePointAt(i);
            switch (state) {
                case INITIAL: {
                    switch (ch) {
                        case '$': {
                            state = GOT_DOLLAR;
                            continue;
                        }
                        default: {
                            builder.appendCodePoint(ch);
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_DOLLAR: {
                    switch (ch) {
                        case '$': {
                            builder.appendCodePoint(ch);
                            state = INITIAL;
                            continue;
                        }
                        case '{': {
                            start = i + 1;
                            nameStart = start;
                            state = GOT_OPEN_BRACE;
                            continue;
                        }
                        default: {
                            // invalid; emit and resume
                            builder.append('$').appendCodePoint(ch);
                            state = INITIAL;
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_OPEN_BRACE: {
                    switch (ch) {
                        case ':':
                        case '}':
                        case ',': {
                            final String name = value.substring(nameStart, i).trim();
                            if ("/".equals(name)) {
                                builder.append(File.separator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (":".equals(name)) {
                                builder.append(File.pathSeparator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            }
                            // First check for system property, then env variable
                            String val = (String) properties.get(name);
                            if (val == null && name.startsWith("env."))
                                val = System.getenv(name.substring(4));

                            if (val != null) {
                                builder.append(val);
                                resolvedValue = val;
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (ch == ',') {
                                nameStart = i + 1;
                                continue;
                            } else if (ch == ':') {
                                start = i + 1;
                                state = DEFAULT;
                                continue;
                            } else {
                                throw new IllegalStateException("Failed to resolve expression: " + value.substring(start - 2, i + 1));
                            }
                        }
                        default: {
                            continue;
                        }
                    }
                    // not reachable
                }
                case RESOLVED: {
                    if (ch == '}') {
                        state = INITIAL;
                    }
                    continue;
                }
                case DEFAULT: {
                    if (ch == '}') {
                        state = INITIAL;
                        builder.append(value.substring(start, i));
                    }
                    continue;
                }
                default:
                    throw new IllegalStateException("Unexpected char seen: " + ch);
            }
        }
        switch (state) {
            case GOT_DOLLAR: {
                builder.append('$');
                break;
            }
            case DEFAULT: {
                builder.append(value.substring(start - 2));
                break;
            }
            case GOT_OPEN_BRACE: {
                // We had a reference that was not resolved, throw ISE
                if (resolvedValue == null)
                    throw new IllegalStateException("Incomplete expression: " + builder.toString());
                break;
            }
        }
        return builder.toString();
    }

}
