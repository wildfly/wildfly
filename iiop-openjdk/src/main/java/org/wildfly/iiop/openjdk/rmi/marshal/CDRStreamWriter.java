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
package org.wildfly.iiop.openjdk.rmi.marshal;

import org.omg.CORBA_2_3.portable.OutputStream;

/**
 * Interface of an object that knows how to marshal a Java basic type or
 * object into a CDR input stream. Implementations of this interface are
 * specialized for particular types: an <code>IntWriter</code> is a
 * <code>CDRStreamWriter</code> that knows how to marshal <code>int</code>s,
 * a <code>LongWriter</code> is a <code>CDRStreamWriter</code> that knows how
 * to marshal <code>long</code>s, and so on.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 81018 $
 */
public interface CDRStreamWriter {
    /**
     * Marshals a Java basic data type or object into a CDR output stream.
     *
     * @param out the output stream
     * @param obj the basic data type (within a suitable wrapper instance)
     *            or object to be marshalled
     */
    void write(OutputStream out, Object obj);
}
