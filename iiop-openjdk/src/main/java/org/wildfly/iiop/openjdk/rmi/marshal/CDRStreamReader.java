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

import org.omg.CORBA_2_3.portable.InputStream;

/**
 * Interface of an object that knows how to unmarshal a Java basic type or
 * object from a CDR input stream. Implementations of this interface are
 * specialized for particular types: an <code>IntReader</code> is a
 * <code>CDRStreamReader</code> that knows how to unmarshal <code>int</code>s,
 * a <code>LongReader</code> is a <code>CDRStreamReader</code> that knows how
 * to unmarshal <code>long</code>s, and so on.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 81018 $
 */
public interface CDRStreamReader {
    /**
     * Unmarshals a Java basic data type or object from a CDR input stream.
     *
     * @param in the input stream
     * @return a basic data type (within a suitable wrapper instance) or
     *         object unmarshalled from the stream
     */
    Object read(InputStream in);
}
