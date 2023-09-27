/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.csiv2;

import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.PolicyFactory;

/**
 * <p>
 * This class implements a {@code org.omg.PortableInterceptor.PolicyFactory} that creates {@code CSIv2Policy} policies.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class CSIv2PolicyFactory extends LocalObject implements PolicyFactory {

    private Codec codec;

    /**
     * <p>
     * Creates an instance of {@code CSIv2PolicyFactory} with the specified codec.
     * </p>
     *
     * @param codec the {@code Codec} used to encode the CSIv2 policies.
     */
    public CSIv2PolicyFactory(Codec codec) {
        this.codec = codec;
    }

    @Override
    public Policy create_policy(int type, Any value) throws PolicyError {
        if (type != CSIv2Policy.TYPE) {
            throw new PolicyError();
        }

        // stored as java.io.Serializable - is this a hack?
        IORSecurityConfigMetaData metadata = (IORSecurityConfigMetaData) value.extract_Value();
        return new CSIv2Policy(metadata, codec);
    }
}
