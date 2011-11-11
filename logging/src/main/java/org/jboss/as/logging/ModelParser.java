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

package org.jboss.as.logging;

import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;

import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 10.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class ModelParser {

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)([kKmMgGbBtT])?");

    private ModelParser() {
    }


    /**
     * Parses the string value of the size and returns the long value.
     *
     * @param node the node that contains the value.
     *
     * @return the long value.
     *
     * @throws IllegalArgumentException if the value is invalid.
     * @throws IllegalStateException    if the ending character is not valid.
     */
    public static long parseSize(final ModelNode node) {
        final Matcher matcher = SIZE_PATTERN.matcher(node.asString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        long qty = Long.parseLong(matcher.group(1), 10);
        final String chr = matcher.group(2);
        if (chr != null) {
            switch (chr.charAt(0)) {
                case 'b':
                case 'B':
                    break;
                case 'k':
                case 'K':
                    qty <<= 10L;
                    break;
                case 'm':
                case 'M':
                    qty <<= 20L;
                    break;
                case 'g':
                case 'G':
                    qty <<= 30L;
                    break;
                case 't':
                case 'T':
                    qty <<= 40L;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        return qty;
    }

    /**
     * Parses the value into a logging level.
     *
     * @param node the node that contains the value.
     *
     * @return the logging level.
     *
     * @throws IllegalArgumentException if the value is invalid.
     */
    public static Level parseLevel(final ModelNode node) {
        return Level.parse(node.asString().toUpperCase(Locale.US));
    }

    /**
     * Parses the string value into an {@link OverflowAction}.
     *
     * @param node the node that contains the value.
     *
     * @return the overflow action.
     *
     * @throws IllegalArgumentException if the value is invalid.
     */
    public static OverflowAction parseOverflowAction(final ModelNode node) {
        return OverflowAction.valueOf(node.asString().toUpperCase(Locale.US));
    }
}
