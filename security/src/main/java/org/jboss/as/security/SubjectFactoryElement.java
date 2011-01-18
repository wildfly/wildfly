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

package org.jboss.as.security;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Representation of the subject factory sub element
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class SubjectFactoryElement extends AbstractModelElement<SubjectFactoryElement> {

    private static final long serialVersionUID = 594455531798984229L;

    private final String subjectFactoryClassName;

    public SubjectFactoryElement(final String subjectFactoryClassName) {
        this.subjectFactoryClassName = subjectFactoryClassName;
    }

    /** {@inheritDoc} */
    protected Class<SubjectFactoryElement> getElementClass() {
        return SubjectFactoryElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // only write attributes when they are not set to the default values
        if (!"default".equals(subjectFactoryClassName))
            streamWriter.writeAttribute(Attribute.SUBJECT_FACTORY_CLASS_NAME.getLocalName(), subjectFactoryClassName);
        streamWriter.writeEndElement();
    }

    /**
     * Check if element has default values for all attributes
     * @return true if element has default values
     */
    boolean isStandard() {
        if (!"default".equals(subjectFactoryClassName))
            return false;
        return true;
    }

    /**
     * Creates an update from the values of the element
     * @return an update
     */
    AddSubjectFactoryUpdate asUpdate() {
        return new AddSubjectFactoryUpdate(subjectFactoryClassName);
    }

}
