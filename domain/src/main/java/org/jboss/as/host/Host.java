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

package org.jboss.as.host;

import java.util.Collection;
import org.jboss.as.domain.Domain;
import org.jboss.as.model.AbstractModel;
import org.jboss.as.parser.DomainElement;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Host extends AbstractModel<Host> {

    private static final long serialVersionUID = 7667892965813702351L;

    protected Host() {
    }

    public long elementHash() {
        return 0;
    }

    public Collection<AbstractHostUpdate<?>> getDifference(final Host other) {
        return null;
    }

    /** {@inheritDoc}  Host elements are always the same because it is the root element of the model. */
    public boolean isSameElement(final Host other) {
        return true;
    }

    protected void addElement(AbstractHostElement<?> hostElement) {
        super.addElement(hostElement);
    }

    protected void removeElement(AbstractHostElement<?> hostElement) {
        super.removeElement(hostElement);
    }

    public Host clone() {
        return super.clone();
    }

    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeComment(
                "!!! NOTE !!!\n\n" +
                "This file is generated and managed by the\n" +
                "Server Manager and should only be edited when\n" +
                "it is offline."
        );
        streamWriter.writeStartElement(Domain.NAMESPACE, DomainElement.DOMAIN.getLocalName());
        streamWriter.writeEndElement();
    }
}
