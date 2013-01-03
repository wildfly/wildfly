/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.allowedmethods;

import org.jboss.as.ee.component.interceptors.InvocationType;

/**
 * @author Stuart Douglas
 */
public final class DeniedMethodKey {

    private final InvocationType invocationType;
    private final MethodType methodType;

    public DeniedMethodKey(InvocationType invocationType, MethodType methodType) {
        this.invocationType = invocationType;
        this.methodType = methodType;
    }

    public InvocationType getInvocationType() {
        return invocationType;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeniedMethodKey deniedMethodKey = (DeniedMethodKey) o;

        if (invocationType != deniedMethodKey.invocationType) return false;
        if (methodType != deniedMethodKey.methodType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = invocationType != null ? invocationType.hashCode() : 0;
        result = 31 * result + (methodType != null ? methodType.hashCode() : 0);
        return result;
    }
}
