/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.jboss.as.parser.DomainElement;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A deployment which is mapped into a {@link DomainServerGroup}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainServerGroupDeployment extends AbstractDomainElement<DomainServerGroupDeployment> {
    private static final long serialVersionUID = -7282640684801436543L;

    private final String deploymentName;
    private final byte[] deploymentHash;
    // todo: deployment overrides

    public DomainServerGroupDeployment(final String deploymentName, final byte[] deploymentHash) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName is null");
        }
        if (deploymentHash == null) {
            throw new IllegalArgumentException("deploymentHash is null");
        }
        if (deploymentHash.length != 20) {
            throw new IllegalArgumentException("deploymentHash is not a valid length");
        }
        this.deploymentName = deploymentName;
        this.deploymentHash = deploymentHash;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        final byte[] hash = deploymentHash;
        return deploymentName.hashCode() & 0xFFFFFFFFL ^ elementHashOf(hash);
    }

    public DomainServerGroupDeployment clone() {
        return super.clone();
    }


    public Collection<? extends AbstractDomainUpdate<?>> getDifference(final DomainServerGroupDeployment other) {
        assert isSameElement(other);
        return Collections.emptySet();
    }

    public boolean isSameElement(final DomainServerGroupDeployment other) {
        return deploymentName.equals(other.deploymentName) && Arrays.equals(deploymentHash, other.deploymentHash);
    }

    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEmptyElement(Domain.NAMESPACE, DomainElement.DEPLOYMENT.getLocalName());
    }
}
