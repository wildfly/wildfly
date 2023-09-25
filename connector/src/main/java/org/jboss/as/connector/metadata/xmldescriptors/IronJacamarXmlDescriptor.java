/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.metadata.xmldescriptors;

import java.io.Serializable;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;

/**
 * A RaXmlDescriptor.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 */
public final class IronJacamarXmlDescriptor implements Serializable {

    private static final long serialVersionUID = 3148478338698997486L;

    public static final AttachmentKey<IronJacamarXmlDescriptor> ATTACHMENT_KEY = AttachmentKey
            .create(IronJacamarXmlDescriptor.class);

    private final Activation ironJacamar;

    /**
     * Create a new RaXmlDescriptor.
     * @param ironJacamar
     */
    public IronJacamarXmlDescriptor(Activation ironJacamar) {
        super();
        this.ironJacamar = ironJacamar;
    }

    /**
     * Get the resource adapters.
     * @return the resource adapters.
     */
    public Activation getIronJacamar() {
        return ironJacamar;
    }

}
