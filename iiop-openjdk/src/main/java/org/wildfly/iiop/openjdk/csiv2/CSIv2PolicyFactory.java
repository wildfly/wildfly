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
