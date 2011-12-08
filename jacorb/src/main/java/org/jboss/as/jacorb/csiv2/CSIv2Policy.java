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
import org.jboss.as.jacorb.service.CorbaORBService;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

/**
 * <p>
 * Implements a {@code org.omg.CORBA.Policy} that stores CSIv2 IOR security info.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CSIv2Policy extends LocalObject implements Policy {

    private static final Logger log = Logger.getLogger("org.jboss.as.jacorb");

    // TODO: contact request@omg.org to get a policy type
    public static final int TYPE = 0x87654321;

    private TaggedComponent sslTaggedComponent;
    private TaggedComponent secTaggedComponent;

    /**
     * <p>
     * Creates an instance of {@code CSIv2Policy} with the specified tagged compoonents.
     * </p>
     *
     * @param sslTaggedComponent a {@code TaggedComponent} that contains the encoded SSL info.
     * @param secTaggedComponent a {@code TaggedComponent} that contains the encoded CSIv2 security info.
     */
    public CSIv2Policy(TaggedComponent sslTaggedComponent, TaggedComponent secTaggedComponent) {
        this.sslTaggedComponent = sslTaggedComponent;
        this.secTaggedComponent = secTaggedComponent;
    }

    /**
     * <p>
     * Creates an instance of {@code CSIv2Policy} with the specified metadata and codec.
     * </p>
     *
     * @param metadata an object containing all the CSIv2 security info.
     * @param codec    the {@code Codec} used to encode the metadata when creating the tagged components.
     */
    public CSIv2Policy(IORSecurityConfigMetaData metadata, Codec codec) {
        if (log.isDebugEnabled()) {
            log.debug(metadata);
        }

        // convert the ior metadata to a cached security tagged component.
        try {
            // get the singleton orb.
            ORB orb = ORB.init();
            String sslPortString = CorbaORBService.getORBProperty(JacORBSubsystemConstants.ORB_SSL_PORT);
            int sslPort = sslPortString == null ? 0 : Integer.parseInt(sslPortString);
            this.sslTaggedComponent = CSIv2Util.createSSLTaggedComponent(metadata, codec, sslPort, orb);
            this.secTaggedComponent = CSIv2Util.createSecurityTaggedComponent(metadata, codec, sslPort, orb);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception " + e);
        }
    }

    /**
     * <p>
     * Return a copy of the cached SSL {@code TaggedComponent}.
     * </p>
     *
     * @return a copy of the cached SSL {@code TaggedComponent}.
     */
    public TaggedComponent getSSLTaggedComponent() {
        return CSIv2Util.createCopy(this.sslTaggedComponent);
    }

    /**
     * <p>
     * Return a copy of the cached CSI {@code TaggedComponent}.
     * </p>
     *
     * @return a copy of the cached CSI {@code TaggedComponent}.
     */
    public TaggedComponent getSecurityTaggedComponent() {
        return CSIv2Util.createCopy(this.secTaggedComponent);
    }

    @Override
    public Policy copy() {
        return new CSIv2Policy(getSSLTaggedComponent(),
                getSecurityTaggedComponent());
    }

    @Override
    public void destroy() {
        this.sslTaggedComponent = null;
        this.secTaggedComponent = null;
    }

    @Override
    public int policy_type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "CSIv2Policy[" + this.sslTaggedComponent + ", "
                + this.secTaggedComponent + "]";
    }
}
