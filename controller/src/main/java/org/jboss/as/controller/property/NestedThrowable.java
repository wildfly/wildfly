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
package org.jboss.as.controller.property;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;


/**
 * Interface which is implemented by all the nested throwable flavors.
 *
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public interface NestedThrowable extends Serializable {
    /**
     * A system wide flag to enable or disable printing of the parent throwable traces.
     *
     * <p>
     * This value is set from the system property <tt>org.jboss.util.NestedThrowable.parentTraceEnabled</tt> or if that is not
     * set defaults to <tt>true</tt>.
     */
    boolean PARENT_TRACE_ENABLED = Util.getBoolean("parentTraceEnabled", true);

    /**
     * A system wide flag to enable or disable printing of the nested detail throwable traces.
     *
     * <p>
     * This value is set from the system property <tt>org.jboss.util.NestedThrowable.nestedTraceEnabled</tt> or if that is not
     * set defaults to <tt>true</tt> unless using JDK 1.4 with {@link #PARENT_TRACE_ENABLED} set to false, then <tt>false</tt>
     * since there is a native mechansim for this there.
     *
     * <p>
     * Note then when running under 1.4 is is not possible to disable the nested trace output, since that is handled by
     * java.lang.Throwable which we delegate the parent printing to.
     */
    boolean NESTED_TRACE_ENABLED = Util.getBoolean("nestedTraceEnabled", true);

    /**
     * A system wide flag to enable or disable checking of parent and child types to detect uneeded nesting
     *
     * <p>
     * This value is set from the system property <tt>org.jboss.util.NestedThrowable.detectDuplicateNesting</tt> or if that is
     * not set defaults to <tt>true</tt>.
     */
    boolean DETECT_DUPLICATE_NESTING = Util.getBoolean("detectDuplicateNesting", true);

    /**
     * Return the nested throwable.
     *
     * @return Nested throwable.
     */
    Throwable getNested();

    /**
     * Return the nested <tt>Throwable</tt>.
     *
     * <p>
     * For JDK 1.4 compatibility.
     *
     * @return Nested <tt>Throwable</tt>.
     */
    Throwable getCause();

    // ///////////////////////////////////////////////////////////////////////
    // Nested Throwable Utilities //
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Utilitiy methods for the various flavors of <code>NestedThrowable</code>.
     */
    final class Util {

        protected static boolean getBoolean(String name, boolean defaultValue) {
            name = NestedThrowable.class.getName() + "." + name;
            String value = System.getProperty(name, String.valueOf(defaultValue));

            return new Boolean(value).booleanValue();
        }

        public static void checkNested(final NestedThrowable parent, final Throwable child) {
            if (!DETECT_DUPLICATE_NESTING || parent == null || child == null)
                return;

            Class<?> parentType = parent.getClass();
            Class<?> childType = child.getClass();
        }

        /**
         * Returns a formated message for the given detail message and nested <code>Throwable</code>.
         *
         * @param msg Detail message.
         * @param nested Nested <code>Throwable</code>.
         * @return Formatted message.
         */
        public static String getMessage(final String msg, final Throwable nested) {
            StringBuffer buff = new StringBuffer(msg == null ? "" : msg);

            if (nested != null) {
                buff.append(msg == null ? "- " : "; - ").append("nested throwable: (").append(nested).append(")");
            }

            return buff.toString();
        }

        /**
         * Prints the nested <code>Throwable</code> to the given stream.
         *
         * @param nested Nested <code>Throwable</code>.
         * @param stream Stream to print to.
         */
        public static void print(final Throwable nested, final PrintStream stream) {
            if (stream == null)
                throw new NullArgumentException("stream");

            if (NestedThrowable.NESTED_TRACE_ENABLED && nested != null) {
                synchronized (stream) {
                    if (NestedThrowable.PARENT_TRACE_ENABLED) {
                        stream.print(" + nested throwable: ");
                    } else {
                        stream.print("[ parent trace omitted ]: ");
                    }

                    nested.printStackTrace(stream);
                }
            }
        }

        /**
         * Prints the nested <code>Throwable</code> to the given writer.
         *
         * @param nested Nested <code>Throwable</code>.
         * @param writer Writer to print to.
         */
        public static void print(final Throwable nested, final PrintWriter writer) {
            if (writer == null)
                throw new NullArgumentException("writer");

            if (NestedThrowable.NESTED_TRACE_ENABLED && nested != null) {
                synchronized (writer) {
                    if (NestedThrowable.PARENT_TRACE_ENABLED) {
                        writer.print(" + nested throwable: ");
                    } else {
                        writer.print("[ parent trace omitted ]: ");
                    }

                    nested.printStackTrace(writer);
                }
            }
        }
    }
}
