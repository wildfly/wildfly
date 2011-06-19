/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.mocks;

import java.io.Serializable;

/**
 * Simple wrapper around a string that will trigger replication on a get if SET_AND_NON_PRIMITIVE_GET is used.
 * 
 * @author Brian Stansberry
 * 
 */
public class MutableObject implements Serializable {
    /** The serialVersionUID */
    private static final long serialVersionUID = -8800180646736265624L;

    private String string;

    /**
     * Create a new MutableObject.
     * 
     */
    public MutableObject(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MutableObject) {
            return this.string.equals(((MutableObject) obj).string);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.string.hashCode();
    }

    @Override
    public String toString() {
        return this.string;
    }

}
