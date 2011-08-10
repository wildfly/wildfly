/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component;

import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * @author Stuart Douglas
 */
public class MethodTransactionAttributeKey {
    private final MethodIntf methodIntf;
    private final MethodIdentifier methodIdentifier;

    public MethodTransactionAttributeKey(final MethodIntf methodIntf, final MethodIdentifier methodIdentifier) {
        this.methodIntf = methodIntf;
        this.methodIdentifier = methodIdentifier;
    }

    public MethodIdentifier getMethodIdentifier() {
        return methodIdentifier;
    }

    public MethodIntf getMethodIntf() {
        return methodIntf;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MethodTransactionAttributeKey that = (MethodTransactionAttributeKey) o;

        if (!methodIdentifier.equals(that.methodIdentifier)) return false;
        if (methodIntf != that.methodIntf) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = methodIntf.hashCode();
        result = 31 * result + methodIdentifier.hashCode();
        return result;
    }
}
