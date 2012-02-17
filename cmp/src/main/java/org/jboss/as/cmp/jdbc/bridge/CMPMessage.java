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
package org.jboss.as.cmp.jdbc.bridge;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Type safe enumeration of message objects.
 * Used by optimistic lock to lock fields and its values depending
 * on the strategy used.
 *
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class CMPMessage
        implements Serializable {
    // Constants ------------------------------------------------
    private static int nextOrdinal = 0;
    private static final CMPMessage[] VALUES = new CMPMessage[5];

    public static final CMPMessage CHANGED = new CMPMessage("CHANGED");
    public static final CMPMessage ACCESSED = new CMPMessage("ACCESSED");

    private final transient String name;
    private final int ordinal;

    // Constructor ----------------------------------------------
    private CMPMessage(String name) {
        this.name = name;
        this.ordinal = nextOrdinal++;
        VALUES[ordinal] = this;
    }

    // Public ---------------------------------------------------
    public String toString() {
        return name;
    }

    // Package --------------------------------------------------
    Object readResolve()
            throws ObjectStreamException {
        return VALUES[ordinal];
    }
}
