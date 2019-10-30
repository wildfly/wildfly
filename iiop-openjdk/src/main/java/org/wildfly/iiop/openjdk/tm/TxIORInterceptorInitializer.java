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

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Implements an <code>org.omg.PortableInterceptor.ORBinitializer</code> that
 * installs the <code>TxIORInterceptor</code>
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 */
@SuppressWarnings("unused")
public class TxIORInterceptorInitializer extends LocalObject implements ORBInitializer {
    /**
     * @since 4.0.1
     */
    static final long serialVersionUID = 963051265993070280L;

    public TxIORInterceptorInitializer() {
        // do nothing
    }

    public void pre_init(ORBInitInfo info) {
        // do nothing
    }

    public void post_init(ORBInitInfo info) {
        try {
            // Use CDR encapsulation with GIOP 1.0 encoding
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value,
                    (byte) 1, /* GIOP version */
                    (byte) 0  /* GIOP revision*/);
            Codec codec = info.codec_factory().create_codec(encoding);
            info.add_ior_interceptor(new TxIORInterceptor(codec));
        } catch (Exception e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
    }
}