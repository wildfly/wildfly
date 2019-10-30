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
package org.wildfly.iiop.openjdk.tm;

import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Implements an <code>org.omg.PortableInterceptor.IORInterceptor</code>
 * that adds spec defined COSTransaction entries to an IOR.
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 */
@SuppressWarnings("unused")
public class TxIORInterceptor extends LocalObject implements IORInterceptor {
    /**
     * @since 4.0.1
     */
    static final long serialVersionUID = -1165643346307852265L;

    public static final int TAG_OTS_POLICY = 31;
    public static final int TAG_INV_POLICY = 32;
    public static final short EITHER = 0;
    public static final short ADAPTS = 3;

    private Codec codec;

    public TxIORInterceptor(Codec codec) {
        this.codec = codec;
    }


    public String name() {
        return TxIORInterceptor.class.getName();
    }

    public void destroy() {
    }

    public void establish_components(IORInfo info) {
        try {
            // Invocation Policy = EITHER
            Any any = ORB.init().create_any();
            any.insert_short(EITHER);
            byte[] taggedComponentData = codec.encode_value(any);
            info.add_ior_component(new TaggedComponent(TAG_INV_POLICY, taggedComponentData));
            // OTS Policy = ADAPTS
            any = ORB.init().create_any();
            any.insert_short(ADAPTS);
            taggedComponentData = codec.encode_value(any);
            info.add_ior_component(new TaggedComponent(TAG_OTS_POLICY, taggedComponentData));
        } catch (InvalidTypeForEncoding e) {
            throw IIOPLogger.ROOT_LOGGER.errorEncodingContext(e);
        }
    }
}
