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

package org.jboss.as.jacorb.csiv2.idl;

import org.jboss.as.jacorb.JacORBMessages;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

/**
 * Generated from IDL interface "SASCurrent".
 *
 * @author JacORB IDL compiler V 2.3.0 (JBoss patch 4), 06-Jun-2007
 * @version generated at Apr 17, 2011 3:27:19 PM
 */
@SuppressWarnings("unused")
public final class SASCurrentHelper {

    public static void insert(final Any any, final SASCurrent s) {
        any.insert_Object(s);
    }

    public static SASCurrent extract(final Any any) {
        return narrow(any.extract_Object());
    }

    public static org.omg.CORBA.TypeCode type() {
        return ORB.init().create_interface_tc("IDL:org/jboss/as/jacorb/csiv2/idl/SASCurrent:1.0", "SASCurrent");
    }

    public static String id() {
        return "IDL:org/jboss/as/jacorb/csiv2/idl/SASCurrent:1.0";
    }

    public static SASCurrent read(final InputStream in) {
        throw new org.omg.CORBA.MARSHAL();
    }

    public static void write(final OutputStream _out, final SASCurrent s) {
        throw new org.omg.CORBA.MARSHAL();
    }

    public static SASCurrent narrow(final Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof SASCurrent) {
            return (SASCurrent) obj;
        } else {
            throw JacORBMessages.MESSAGES.sasCurrentNarrowFailed();
        }
    }

    public static SASCurrent unchecked_narrow(final Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof SASCurrent) {
            return (SASCurrent) obj;
        } else {
            throw JacORBMessages.MESSAGES.sasCurrentNarrowFailed();
        }
    }
}
