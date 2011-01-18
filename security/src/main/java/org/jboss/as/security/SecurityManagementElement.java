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
 * Representation of the security management sub element
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class SecurityManagementElement extends AbstractModelElement<SecurityManagementElement> {

    private static final long serialVersionUID = -5531933026432998611L;

    private final String authenticationManagerClassName;

    private final String defaultCallbackHandlerClassName;

    private final boolean deepCopySubjectMode;

    public SecurityManagementElement(final String authenticationManagerClassName, final boolean deepCopySubjectMode,
            final String defaultCallbackHandlerClassName) {
        this.authenticationManagerClassName = authenticationManagerClassName;
        this.deepCopySubjectMode = deepCopySubjectMode;
        this.defaultCallbackHandlerClassName = defaultCallbackHandlerClassName;
    }

    /** {@inheritDoc} */
    protected Class<SecurityManagementElement> getElementClass() {
        return SecurityManagementElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // only write attributes when they are not set to the default values
        if (!"default".equals(authenticationManagerClassName))
            streamWriter.writeAttribute(Attribute.AUTHENTICATION_MANAGER_CLASS_NAME.getLocalName(),
                    authenticationManagerClassName);
        if (deepCopySubjectMode)
            streamWriter.writeAttribute(Attribute.DEEP_COPY_SUBJECT_MODE.getLocalName(), Boolean.TRUE.toString());
        if (!"default".equals(defaultCallbackHandlerClassName))
            streamWriter.writeAttribute(Attribute.DEFAULT_CALLBACK_HANDLER_CLASS_NAME.getLocalName(),
                    defaultCallbackHandlerClassName);
        streamWriter.writeEndElement();
    }

    /**
     * Check if element has default values for all attributes
     *
     * @return true if element has default values
     */
    boolean isStandard() {
        if (!"default".equals(authenticationManagerClassName) || deepCopySubjectMode
                || !"default".equals(defaultCallbackHandlerClassName))
            return false;
        return true;
    }

    /**
     * Creates an update from the values of the element
     *
     * @return an update
     */
    AddSecurityManagementUpdate asUpdate() {
        return new AddSecurityManagementUpdate(authenticationManagerClassName, deepCopySubjectMode,
                defaultCallbackHandlerClassName);
    }

}
