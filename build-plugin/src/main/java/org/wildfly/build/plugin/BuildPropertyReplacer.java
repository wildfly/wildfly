package org.wildfly.build.plugin;

/**
 * @author Stuart Douglas
 */

import java.io.File;
import java.util.Properties;

/**
 * Replace properties of the form:
 * <code>${<i>&lt;[env.]name&gt;[</i>,<i>&lt;[env.]name2&gt;[</i>,<i>&lt;[env.]name3&gt;...]][</i>]</i>}</code>
 *
 * @author Jaikiran Pai (copied from JBoss DMR project)
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author John Bailey
 */
public class BuildPropertyReplacer {

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;
    private static final int RESOLVED = 3;
    private static final int DEFAULT = 4;

    private final Properties properties;

    public BuildPropertyReplacer(Properties properties) {
        this.properties = properties;
    }

    public String replaceProperties(final String value) {
        final StringBuilder builder = new StringBuilder();
        final int len = value.length();
        int state = INITIAL;
        int start = -1;
        int nameStart = -1;
        int expressionStart = -1;
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
                            expressionStart = nameStart = start;
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
                        case '}':
                        case ',': {
                            final String name = value.substring(nameStart, i).trim();
                            if ("/".equals(name)) {
                                builder.append(File.separator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            }
                            final String val = properties.getProperty(name);

                            if (val != null) {
                                builder.append(val);
                                resolvedValue = val;
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (ch == ',') {
                                nameStart = i + 1;
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
                        // JBMETA-371 check in case the whole expression was meant to be resolved
                        final String val = properties.getProperty(value.substring(expressionStart, i));
                        if (val != null) {
                            builder.append(val);
                        } else {
                            builder.append(value.substring(start, i));
                        }
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

