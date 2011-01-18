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

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.security.service.JaasBinderService;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A Security subsystem definition
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class SecuritySubsystemElement extends AbstractSubsystemElement<SecuritySubsystemElement> {

    private static final long serialVersionUID = -8610383890594318467L;

    public static final ServiceName JBOSS_SECURITY = ServiceName.JBOSS.append("security");

    private SecurityManagementElement securityManagement;

    private SubjectFactoryElement subjectFactory;

    private JaasElement jaas;

    /**
     * Create a new instance
     */
    public SecuritySubsystemElement() {
        super(Namespace.SECURITY_1_0.getUriString());
    }

    /** {@inheritDoc} */
    protected <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // remove bootstrap service
        final ServiceController<?> bootStrapService = updateContext.getServiceRegistry().getService(
                SecurityBootstrapService.SERVICE_NAME);
        if (bootStrapService != null) {
            bootStrapService.setMode(Mode.REMOVE);
        }

        // remove jaas binder service
        final ServiceController<?> binderService = updateContext.getServiceRegistry()
                .getService(JaasBinderService.SERVICE_NAME);
        if (binderService != null) {
            binderService.setMode(Mode.REMOVE);
        }
    }

    /** {@inheritDoc} */
    protected AbstractSubsystemAdd<SecuritySubsystemElement> getAdd() {
        final SecuritySubsystemAdd add = new SecuritySubsystemAdd();
        return add;
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<SecuritySubsystemElement, ?>> list) {
        if (securityManagement != null) {
            AddSecurityManagementUpdate update = securityManagement.asUpdate();
            list.add(update);
        }
        if (subjectFactory != null) {
            AddSubjectFactoryUpdate update = subjectFactory.asUpdate();
            list.add(update);
        }
        if (jaas != null) {
            AddJaasUpdate update = jaas.asUpdate();
            list.add(update);
        }
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        if (securityManagement == null && subjectFactory == null && jaas == null)
            return true;
        return false;
    }

    /** {@inheritDoc} */
    protected Class<SecuritySubsystemElement> getElementClass() {
        return SecuritySubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // only writes sub elements if they have custom values
        if (securityManagement != null && !securityManagement.isStandard()) {
            streamWriter.writeStartElement(Element.SECURITY_MANAGEMENT.getLocalName());
            securityManagement.writeContent(streamWriter);
        }
        if (subjectFactory != null && !subjectFactory.isStandard()) {
            streamWriter.writeStartElement(Element.SUBJECT_FACTORY.getLocalName());
            subjectFactory.writeContent(streamWriter);
        }
        if (jaas != null && !jaas.isStandard()) {
            streamWriter.writeStartElement(Element.JAAS.getLocalName());
            jaas.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    /**
     * Sets security management element
     *
     * @param element
     */
    protected void setSecurityManagement(SecurityManagementElement element) {
        securityManagement = element;
    }

    /**
     * Gets the security management element
     *
     * @return the element
     */
    public SecurityManagementElement getSecurityManagement() {
        return securityManagement;
    }

    /**
     * Sets subject factory element
     *
     * @param element
     */
    protected void setSubjectFactory(SubjectFactoryElement element) {
        subjectFactory = element;
    }

    /**
     * Gets the security management element
     *
     * @return the element
     */
    public SubjectFactoryElement getSubjectFactory() {
        return subjectFactory;
    }

    /**
     * Sets jaas element
     *
     * @param element
     */
    protected void setJaas(JaasElement element) {
        jaas = element;
    }

    /**
     * Gets the jaas element
     *
     * @return the element
     */
    public JaasElement getJaas() {
        return jaas;
    }
}
