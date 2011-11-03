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

package org.jboss.as.jacorb.csiv2;

import org.jboss.as.jacorb.JacORBSubsystemConstants;
import org.jboss.as.jacorb.metadata.IORSecurityConfigMetadata;
import org.jboss.as.jacorb.service.CorbaORBService;
import org.jboss.logging.Logger;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CSIIOP.DetectMisordering;
import org.omg.CSIIOP.DetectReplay;
import org.omg.CSIIOP.Integrity;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;
import org.omg.SSLIOP.SSL;
import org.omg.SSLIOP.SSLHelper;
import org.omg.SSLIOP.TAG_SSL_SEC_TRANS;

/**
 * <p>
 * Implements an {@code org.omg.PortableInterceptor.IORInterceptor} that inserts CSIv2 info into an IOR.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CSIv2IORInterceptor extends LocalObject implements IORInterceptor {

    private static final Logger log = Logger.getLogger("org.jboss.as.jacorb");

    // The minimum set of security options supported by the SSL mechanism (These options cannot be turned off, so they are always supported).
    private static final int MIN_SSL_OPTIONS = Integrity.value | DetectReplay.value | DetectMisordering.value;

    private TaggedComponent defaultSSLComponent;

    private TaggedComponent defaultCSIComponent;

    /**
     * <p>
     * Creates an instance of {@code CSIv2IORInterceptor} with the specified codec.
     * </p>
     *
     * @param codec the {@code Codec} used to encode the IOR security components.
     */
    public CSIv2IORInterceptor(Codec codec) {
        String sslPortString = CorbaORBService.getORBProperty(JacORBSubsystemConstants.ORB_SSL_PORT);
        int sslPort = sslPortString == null ? 0 : Integer.parseInt(sslPortString);
        try {
            // build default SSL component with minimum SSL options.
            SSL ssl = new SSL((short) MIN_SSL_OPTIONS, /* supported options */
                    (short) 0, /* required options  */
                    (short) sslPort);
            ORB orb = ORB.init();
            Any any = orb.create_any();
            SSLHelper.insert(any, ssl);
            byte[] componentData = codec.encode_value(any);
            defaultSSLComponent = new TaggedComponent(TAG_SSL_SEC_TRANS.value, componentData);
            defaultCSIComponent = CSIv2Util.createSecurityTaggedComponent(new IORSecurityConfigMetadata(), codec,
                    sslPort, orb);
        } catch (InvalidTypeForEncoding e) {
            log.warn("Caught unexcepted exception while encoding SSL component", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void establish_components(IORInfo info) {
        // check if CSIv2 policy is in effect for this IOR.
        CSIv2Policy csiv2Policy = null;

        try {
            csiv2Policy = (CSIv2Policy) info.get_effective_policy(CSIv2Policy.TYPE);
        } catch (BAD_PARAM e) {
            log.debugf("No CSIv2Policy");
        } catch (Exception e) {
            log.debugf("Error fetching CSIv2Policy", e);
        }

        boolean interopIONA = "on".equalsIgnoreCase(CorbaORBService.getORBProperty(JacORBSubsystemConstants.INTEROP_IONA));
        if (csiv2Policy != null) {
            // if csiv2Policy effective, stuff a copy of the TaggedComponents already created by the CSIv2Policy into the IOR's IIOP profile.
            TaggedComponent sslComponent = csiv2Policy.getSSLTaggedComponent();
            // if interop with IONA ASP is on, don't add the SSL component to the IOR.
            if (sslComponent != null && !interopIONA) {
                info.add_ior_component_to_profile(sslComponent, TAG_INTERNET_IOP.value);
            }
            TaggedComponent csiv2Component = csiv2Policy.getSecurityTaggedComponent();
            if (csiv2Component != null) {
                info.add_ior_component_to_profile(csiv2Component, TAG_INTERNET_IOP.value);
            }
        } else {
            if (defaultSSLComponent != null && !interopIONA) {
                // otherwise stuff the default SSL component (with the minimum set of SSL options) into the IOR's IIOP profile.
                info.add_ior_component_to_profile(defaultSSLComponent, TAG_INTERNET_IOP.value);
            }
            if (defaultCSIComponent != null) {
                // and stuff the default CSI component (with the minimum set of CSI options) into the IOR's IIOP profile.
                info.add_ior_component_to_profile(defaultCSIComponent, TAG_INTERNET_IOP.value);
            }
        }
    }

    @Override
    public String name() {
        return CSIv2IORInterceptor.class.getName();
    }
}