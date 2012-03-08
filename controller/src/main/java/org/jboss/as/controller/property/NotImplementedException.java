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
 * Thrown to indicate that a method has not been implemented yet.
 *
 * <p>
 * This exception is used to help stub out implementations.
 *
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class NotImplementedException extends RuntimeException {
    /** The serialVersionUID */
    private static final long serialVersionUID = -3915801189311749818L;

    /**
     * Construct a <tt>NotImplementedException</tt> with a detail message.
     *
     * @param msg Detail message.
     */
    public NotImplementedException(final String msg) {
        super(msg);
    }

    /**
     * Construct a <tt>NotImplementedException</tt> with no detail.
     */
    public NotImplementedException() {
        super();
    }
}
