/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
