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

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.plugins.JBossSecuritySubjectFactory;

/**
 * Update to add the subject factory service
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class AddSubjectFactoryUpdate extends AbstractSecuritySubsystemUpdate<Void> {

    private static final long serialVersionUID = 2029194703350680474L;

    private String subjectFactoryClassName;

    private static final String SUBJECT_FACTORY = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
            + JBossSecuritySubjectFactory.class.getName();

    public AddSubjectFactoryUpdate(String subjectFactoryClassName) {
        this.subjectFactoryClassName = subjectFactoryClassName;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getServiceTarget();

        // setting default classes
        if (subjectFactoryClassName.equals("default"))
            subjectFactoryClassName = SUBJECT_FACTORY;

        // add subject factory service
        final SubjectFactoryService subjectFactoryService = new SubjectFactoryService(subjectFactoryClassName);
        target.addService(SubjectFactoryService.SERVICE_NAME, subjectFactoryService).addDependency(
                SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                subjectFactoryService.getSecurityManagementInjector()).setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<SecuritySubsystemElement, ?> getCompensatingUpdate(SecuritySubsystemElement original) {
        return new RemoveSubjectFactoryUpdate();
    }

    /** {@inheritDoc} */
    protected void applyUpdate(SecuritySubsystemElement element) throws UpdateFailedException {
        SubjectFactoryElement subjectFactory = new SubjectFactoryElement(subjectFactoryClassName);
        element.setSubjectFactory(subjectFactory);
    }

}
