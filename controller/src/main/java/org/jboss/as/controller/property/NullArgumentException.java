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

/**
 * Thrown to indicate that a method argument was <tt>null</tt> and should <b>not</b> have been.
 *
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class NullArgumentException extends IllegalArgumentException {
    /** The serialVersionUID */
    private static final long serialVersionUID = -2308503249278841584L;
    /** The name of the argument that was <tt>null</tt>. */
    protected final String name;

    /** The index of the argument or null if no index. */
    protected final Object index;

    /**
     * Construct a <tt>NullArgumentException</tt>.
     *
     * @param name Argument name.
     */
    public NullArgumentException(final String name) {
        super(makeMessage(name));

        this.name = name;
        this.index = null;
    }

    /**
     * Construct a <tt>NullArgumentException</tt>.
     *
     * @param name Argument name.
     * @param index Argument index.
     */
    public NullArgumentException(final String name, final long index) {
        super(makeMessage(name, new Long(index)));

        this.name = name;
        this.index = new Long(index);
    }

    /**
     * Construct a <tt>NullArgumentException</tt>.
     *
     * @param name Argument name.
     * @param index Argument index.
     */
    public NullArgumentException(final String name, final Object index) {
        super(makeMessage(name, index));

        this.name = name;
        this.index = index;
    }

    /**
     * Construct a <tt>NullArgumentException</tt>.
     */
    public NullArgumentException() {
        this.name = null;
        this.index = null;
    }

    /**
     * Get the argument name that was <tt>null</tt>.
     *
     * @return The argument name that was <tt>null</tt>.
     */
    public final String getArgumentName() {
        return name;
    }

    /**
     * Get the argument index.
     *
     * @return The argument index.
     */
    public final Object getArgumentIndex() {
        return index;
    }

    /**
     * Make a execption message for the argument name.
     */
    private static String makeMessage(final String name) {
        return "'" + name + "' is null";
    }

    /**
     * Make a execption message for the argument name and index
     */
    private static String makeMessage(final String name, final Object index) {
        return "'" + name + "[" + index + "]' is null";
    }
}
