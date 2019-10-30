/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk.csiv2;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * <p>
 * This class implements an {@code org.omg.PortableInterceptor.ORBinitializer} that installs a {@code CSIv2IORInterceptor}
 * and a {@code CSIv2PolicyFactory}.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class CSIv2Initializer extends LocalObject implements ORBInitializer {

    @Override
    public void pre_init(ORBInitInfo info) {
    }

    @Override
    public void post_init(ORBInitInfo info) {
        try {
            // use CDR encapsulation with GIOP 1.0 encoding.
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value,
                    (byte) 1, /* GIOP version */
                    (byte) 0  /* GIOP revision*/);
            Codec codec = info.codec_factory().create_codec(encoding);

            // add IOR interceptor for CSIv2.
                info.add_ior_interceptor(new CSIv2IORInterceptor(codec));

            // register CSIv2-related policy factories.
            info.register_policy_factory(CSIv2Policy.TYPE, new CSIv2PolicyFactory(codec));
        } catch (Exception e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
    }
}
