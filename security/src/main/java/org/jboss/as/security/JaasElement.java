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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Representation of the jaas sub element
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class JaasElement extends AbstractModelElement<JaasElement> {

    private static final long serialVersionUID = -1731153272201009396L;

    private List<ApplicationPolicy> applicationPolicies;

    /** {@inheritDoc} */
    protected Class<JaasElement> getElementClass() {
        return JaasElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (applicationPolicies != null) {
            for (ApplicationPolicy applicationPolicy : applicationPolicies) {
                applicationPolicy.writeContent(streamWriter);
            }
        }
        streamWriter.writeEndElement();
    }

    /**
     * Check if element has default values for all attributes
     *
     * @return true if element has default values
     */
    boolean isStandard() {
        if (applicationPolicies == null || applicationPolicies.size() == 0)
            return true;
        return false;
    }

    /**
     * Creates an update from the values of the element
     *
     * @return an update
     */
    AddJaasUpdate asUpdate() {
        AddJaasUpdate jaasUpdate = new AddJaasUpdate();
        jaasUpdate.setApplicationPolicies(applicationPolicies);
        return jaasUpdate;
    }

    /**
     * Returns a list with the registered application policies
     *
     * @return security domains
     */
    public List<ApplicationPolicy> getApplicationPolicies() {
        return applicationPolicies;
    }

    /**
     * Sets the list of application policies
     *
     * @param securityDomains list
     */
    public void setApplicationPolicies(List<ApplicationPolicy> applicationPolicies) {
        this.applicationPolicies = applicationPolicies;
    }

    /**
     * Adds a new application policy to the list
     *
     * @param applicationPolicy new security domain
     */
    public void addApplicationPolicy(ApplicationPolicy applicationPolicy) {
        if (applicationPolicies == null)
            applicationPolicies = new ArrayList<ApplicationPolicy>();
        applicationPolicies.add(applicationPolicy);
    }

}
