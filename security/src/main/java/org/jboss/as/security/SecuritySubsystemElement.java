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
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.security.service.SubjectFactoryService;
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

    private String authenticationManagerClassName;

    private boolean deepCopySubjectMode;

    private String defaultCallbackHandlerClassName;

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

        // remove security management service
        final ServiceController<?> securityManagementService = updateContext.getServiceRegistry().getService(
                SecurityManagementService.SERVICE_NAME);
        if (securityManagementService != null) {
            securityManagementService.setMode(Mode.REMOVE);
        }

        // remove jaas binder service
        final ServiceController<?> binderService = updateContext.getServiceRegistry()
                .getService(JaasBinderService.SERVICE_NAME);
        if (binderService != null) {
            binderService.setMode(Mode.REMOVE);
        }

        // remove subject factory service
        final ServiceController<?> subjectFactoryService = updateContext.getServiceRegistry().getService(
                SubjectFactoryService.SERVICE_NAME);
        if (subjectFactoryService != null) {
            subjectFactoryService.setMode(Mode.REMOVE);
        }
    }

    /** {@inheritDoc} */
    protected AbstractSubsystemAdd<SecuritySubsystemElement> getAdd() {
        final SecuritySubsystemAdd add = new SecuritySubsystemAdd(authenticationManagerClassName, deepCopySubjectMode,
                defaultCallbackHandlerClassName);
        return add;
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<SecuritySubsystemElement, ?>> list) {
        // no sub elements yet
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        return true;
    }

    /** {@inheritDoc} */
    protected Class<SecuritySubsystemElement> getElementClass() {
        return SecuritySubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // only write attributes when they are not set to the default values
        if (!SecuritySubsystemParser.defaultAuthenticationManagerClassName.equals(authenticationManagerClassName))
            streamWriter.writeAttribute(Attribute.AUTHENTICATION_MANAGER_CLASS_NAME.getLocalName(),
                    authenticationManagerClassName);
        if (deepCopySubjectMode)
            streamWriter.writeAttribute(Attribute.DEEP_COPY_SUBJECT_MODE.getLocalName(), Boolean.TRUE.toString());
        if (!SecuritySubsystemParser.defaultCallbackHandlerClassName.equals(defaultCallbackHandlerClassName))
            streamWriter.writeAttribute(Attribute.DEFAULT_CALLBACK_HANDLER_CLASS_NAME.getLocalName(),
                    defaultCallbackHandlerClassName);
        streamWriter.writeEndElement();
    }

    /**
     * Get the class name of the {@code AuthenticationManager} implementation to be used
     *
     * @return the class name
     */
    public String getAuthenticationManagerClassName() {
        return authenticationManagerClassName;
    }

    /**
     * Set the class name of the {@code AuthenticationManager} implementation to be used
     *
     * @param authenticationManagerClassName the class name
     */
    public void setAuthenticationManagerClassName(String authenticationManagerClassName) {
        this.authenticationManagerClassName = authenticationManagerClassName;
    }

    /**
     * Get the flag indicating the copy mode
     *
     * @return the flag
     */
    public boolean isDeepCopySubjectMode() {
        return deepCopySubjectMode;
    }

    /**
     * Set the flag indicating the copy mode
     *
     * @param deepCopySubjectMode the flag
     */
    public void setDeepCopySubjectMode(boolean deepCopySubjectMode) {
        this.deepCopySubjectMode = deepCopySubjectMode;
    }

    /**
     * Get the class name of the {@code CallbackHandler} implementation to be used
     *
     * @return the class name
     */
    public String getDefaultCallbackHandlerClassName() {
        return defaultCallbackHandlerClassName;
    }

    /**
     * Set the class name of the {@code CallbackHandler} implementation to be used
     *
     * @param defaultCallbackHandlerClassName the class name
     */
    public void setDefaultCallbackHandlerClassName(String defaultCallbackHandlerClassName) {
        this.defaultCallbackHandlerClassName = defaultCallbackHandlerClassName;
    }

}
