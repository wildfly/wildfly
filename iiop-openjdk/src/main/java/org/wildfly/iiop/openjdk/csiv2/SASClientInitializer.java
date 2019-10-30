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
 * <p>
 * This is an {@code org.omg.PortableInterceptor.ORBInitializer} that initializes the Security Attibute Service (SAS).
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class SASClientInitializer extends LocalObject implements ORBInitializer {

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
            SASClientInterceptor clientInterceptor = new SASClientInterceptor(codec);
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
