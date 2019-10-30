/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
