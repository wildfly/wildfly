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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.plugins.JBossAuthorizationManager;
import org.jboss.security.plugins.auth.JaasSecurityManagerBase;

/**
 * Update to add the security management service
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class AddSecurityManagementUpdate extends AbstractSecuritySubsystemUpdate<Void> {

    private static final long serialVersionUID = -1447351678871557072L;

    private String authenticationManagerClassName;

    private String defaultCallbackHandlerClassName;

    private boolean deepCopySubjectMode;

    private static final String AUTHENTICATION_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot()
            + ":" + JaasSecurityManagerBase.class.getName();

    private static final String CALLBACK_HANDLER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
            + JBossCallbackHandler.class.getName();

    private static final String AUTHORIZATION_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot()
            + ":" + JBossAuthorizationManager.class.getName();

    public AddSecurityManagementUpdate(String authenticationManagerClassName, boolean deepCopySubjectMode,
            String defaultCallbackHandlerClassName) {
        this.authenticationManagerClassName = authenticationManagerClassName;
        this.deepCopySubjectMode = deepCopySubjectMode;
        this.defaultCallbackHandlerClassName = defaultCallbackHandlerClassName;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getServiceTarget();

        // setting default classes
        if (authenticationManagerClassName.equals("default"))
            authenticationManagerClassName = AUTHENTICATION_MANAGER;
        if (defaultCallbackHandlerClassName.equals("default"))
            defaultCallbackHandlerClassName = CALLBACK_HANDLER;

        // add security management service
        final SecurityManagementService securityManagementService = new SecurityManagementService(
                authenticationManagerClassName, deepCopySubjectMode, defaultCallbackHandlerClassName, AUTHORIZATION_MANAGER);
        target.addService(SecurityManagementService.SERVICE_NAME, securityManagementService).addListener(
                new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param)).setInitialMode(
                ServiceController.Mode.ACTIVE).install();

    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<SecuritySubsystemElement, ?> getCompensatingUpdate(SecuritySubsystemElement original) {
        return new RemoveSecurityManagementUpdate();
    }

    /** {@inheritDoc} */
    protected void applyUpdate(SecuritySubsystemElement element) throws UpdateFailedException {
        SecurityManagementElement securityManagement = new SecurityManagementElement(authenticationManagerClassName,
                deepCopySubjectMode, defaultCallbackHandlerClassName);
        element.setSecurityManagement(securityManagement);
    }

}
