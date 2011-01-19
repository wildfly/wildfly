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

package org.jboss.as.osgi.parser;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.osgi.deployment.OSGiDeploymentActivator;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.service.BundleContextService;
import org.jboss.as.osgi.service.BundleManagerService;
import org.jboss.as.osgi.service.ConfigAdminServiceImpl;
import org.jboss.as.osgi.service.FrameworkService;
import org.jboss.as.osgi.service.PackageAdminService;
import org.jboss.as.osgi.service.StartLevelService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceTarget;

/**
 * OSGi subsystem add element.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public final class OSGiSubsystemAdd extends AbstractSubsystemAdd<OSGiSubsystemElement> {

    private static final long serialVersionUID = -4542570180370773590L;
    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final SubsystemState subsystemState = new SubsystemState();

    protected OSGiSubsystemAdd() {
        super(OSGiExtension.NAMESPACE);
    }

    @Override
    protected OSGiSubsystemElement createSubsystemElement() {
        return new OSGiSubsystemElement();
    }

    SubsystemState getSubsystemState() {
        return subsystemState;
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        log.infof("Activating OSGi Subsystem");

        // TODO: Hack, which registers the framework module with the {@link ModularURLStreamHandlerFactory}
        // TODO - use an actually secure sys prop security action method
        String value = SecurityActions.getSystemProperty("jboss.protocol.handler.modules", "org.jboss.osgi.framework");
        if (!value.equals("org.jboss.osgi.framework"))
            value = value + "|org.jboss.osgi.framework";
        SecurityActions.setSystemProperty("jboss.protocol.handler.modules", value);

        Activation policy = subsystemState.getActivationPolicy();
        ServiceTarget target = updateContext.getServiceTarget();
        BundleManagerService.addService(target, subsystemState);
        FrameworkService.addService(target, subsystemState);
        BundleContextService.addService(target, policy);
        PackageAdminService.addService(target);
        StartLevelService.addService(target);

        ConfigAdminServiceImpl.addService(target, subsystemState);
    }

    protected void applyUpdateBootAction(BootUpdateContext updateContext) {
        applyUpdate(updateContext, UpdateResultHandler.NULL, null);
        new OSGiDeploymentActivator().activate(updateContext);
    }
}
