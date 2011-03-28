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

package org.jboss.as.jpa.ejb3;

import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.jpa.spi.SFSBContextHandle;

import java.io.Serializable;

/**
 * Implementation of SFSBContextHandle that represents an EJB3 SFSB
 *
 * @author Scott Marlow
 */
public class SFSBContextHandleImpl implements SFSBContextHandle {

    private Serializable idSFSB;

    public SFSBContextHandleImpl(StatefulSessionComponentInstance sfsb) {
        this.idSFSB = sfsb.getId();
    }
    @Override
    public Object getBeanContextHandle() {
        return idSFSB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SFSBContextHandleImpl that = (SFSBContextHandleImpl) o;

        if (idSFSB != null ? !idSFSB.equals(that.idSFSB) : that.idSFSB != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return idSFSB != null ? idSFSB.hashCode() : 0;
    }
}
