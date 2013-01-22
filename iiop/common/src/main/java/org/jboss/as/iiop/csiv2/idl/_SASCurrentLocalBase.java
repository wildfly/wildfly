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

package org.jboss.as.iiop.csiv2.idl;

import org.omg.CORBA.LocalObject;

/**
 * Abstract base class for implementations of local interface SASCurrent
 *
 * @author JacORB IDL compiler.
 */
public abstract class _SASCurrentLocalBase extends LocalObject implements SASCurrent {

    private String[] _type_ids = {"IDL:org/jboss/as/iiop/csiv2/idl/SASCurrent:1.0", "IDL:omg.org/CORBA/Current:1.0"};

    public String[] _ids() {
        return _type_ids.clone();
    }
}
