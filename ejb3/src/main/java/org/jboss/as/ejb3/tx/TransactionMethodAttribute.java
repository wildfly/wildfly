/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.tx;

import java.util.concurrent.TimeUnit;

import javax.ejb.TransactionAttributeType;

public class TransactionMethodAttribute {
    public static final TransactionMethodAttribute REQUIRED = new TransactionMethodAttribute(TransactionAttributeType.REQUIRED);

    private final TransactionAttributeType type;

    private final long timeout;

    private final TimeUnit unit;

    public TransactionMethodAttribute(final TransactionAttributeType type) {
        this(type, -1, TimeUnit.SECONDS);
    }

    public TransactionMethodAttribute(final TransactionAttributeType type, final long timeout, final TimeUnit unit) {
        this.type = type;
        this.timeout = timeout;
        this.unit = unit;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public long getTimeout() {
        return timeout;
    }

    public TransactionAttributeType getType() {
        return type;
    }
}
