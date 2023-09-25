/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.csiv2;

import org.jboss.iiop.csiv2.SASCurrent;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;
import org.omg.PortableInterceptor.ORBInitializer;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * This is an {@link ORBInitializer} that initializes the Security Attibute Service (SAS) by installing an Elytron-based
 * client side interceptor and a SAS target interceptor that is used to populate the {@link SASCurrent} object.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class ElytronSASInitializer extends LocalObject implements ORBInitializer {

    @Override
    public void pre_init(ORBInitInfo info) {
        try {
            // create and register the SASCurrent.
            SASCurrent sasCurrent = new SASCurrentImpl();
            info.register_initial_reference("SASCurrent", sasCurrent);

            // the SASCurrent still needs to be initialized. Its initialization is deferred to post_init, as it needs
            // to call resolve_initial_references.
        } catch (InvalidName e) {
            throw IIOPLogger.ROOT_LOGGER.errorRegisteringSASCurrentInitRef(e);
        }
    }

    @Override
    public void post_init(ORBInitInfo info) {
        try {
            org.omg.CORBA.Object obj;

            // Use CDR encapsulations with GIOP 1.0 encoding.
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value,
                    (byte) 1, /* GIOP version */
                    (byte) 0  /* GIOP revision*/);
            Codec codec = info.codec_factory().create_codec(encoding);

            // Create and register client interceptor.
            obj = info.resolve_initial_references("SASCurrent");
            SASCurrentImpl sasCurrentImpl = (SASCurrentImpl) obj;
            ElytronSASClientInterceptor clientInterceptor = new ElytronSASClientInterceptor(codec);
            info.add_client_request_interceptor(clientInterceptor);

            // Create and register server interceptor.
            SASTargetInterceptor serverInterceptor = new SASTargetInterceptor(codec);
            info.add_server_request_interceptor(serverInterceptor);

            // Initialize the SASCurrent implementation.
            sasCurrentImpl.init(serverInterceptor);
        } catch (Exception e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
    }
}
