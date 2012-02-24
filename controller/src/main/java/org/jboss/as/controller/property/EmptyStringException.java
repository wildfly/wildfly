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
 * Thrown to indicate that a string was empty (aka. <code>""</code>) where it must <b>not</b> be.
 *
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class EmptyStringException extends IllegalArgumentException {
    /** The serialVersionUID */
    private static final long serialVersionUID = -7958716001355854762L;

    /**
     * Construct a <tt>EmptyStringException</tt>.
     *
     * @param msg Exception message.
     */
    public EmptyStringException(final String msg) {
        super(msg);
    }

    /**
     * Construct a <tt>EmptyStringException</tt>.
     */
    public EmptyStringException() {
        super();
    }
}
