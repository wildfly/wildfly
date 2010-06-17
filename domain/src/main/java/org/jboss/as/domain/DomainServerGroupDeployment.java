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

import java.util.Collection;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A deployment which is mapped into a {@link DomainServerGroup}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainServerGroupDeployment extends AbstractDomainElement<DomainServerGroupDeployment> {
    private static final long serialVersionUID = -7282640684801436543L;

    private final AbstractDomainDeployment domainDeployment;
    private final DomainServerGroup domainServerGroup;

    public DomainServerGroupDeployment(final String id, final AbstractDomainDeployment domainDeployment, final DomainServerGroup domainServerGroup) {
        super(id);
        this.domainDeployment = domainDeployment;
        this.domainServerGroup = domainServerGroup;
    }

    public long checksum() {
        return 0;
    }

    public Collection<? extends AbstractDomainUpdate<?>> getDifference(final DomainServerGroupDeployment other) {
        return null;
    }

    public void writeObject(final XMLStreamWriter streamWriter) throws XMLStreamException {
    }
}
