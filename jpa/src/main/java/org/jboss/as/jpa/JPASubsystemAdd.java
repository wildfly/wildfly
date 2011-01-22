/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa;

import org.jboss.as.jpa.config.JBossAssemblyDescriptor;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Add the JPA subsystem directive.
 * <p/>
 * TODO:  add subsystem configuration properties
 *
 * @author Scott Marlow
 */
public class JPASubsystemAdd extends AbstractSubsystemAdd<JPASubsystemElement> {
    private JBossAssemblyDescriptor assemblyDescriptor;

    protected JPASubsystemAdd() {
        super(JPASubsystemParser.NAMESPACE);
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
    }

    @Override
    protected void applyUpdateBootAction(BootUpdateContext updateContext) {

        JPADeploymentActivator.activate(updateContext);

        // TODO:  figure out what the deal is with the following call, which calls the above applyUpdate().
        //        WebSubsystemAdd doesn't call super.applyUpdateBootAction, maybe we shouldn't either?
        super.applyUpdateBootAction(updateContext);
    }

    @Override
    protected JPASubsystemElement createSubsystemElement() {
        return new JPASubsystemElement();
    }

    protected JBossAssemblyDescriptor getAssemblyDescriptor() {
        return assemblyDescriptor;
    }

    protected void setAssemblyDescriptor(JBossAssemblyDescriptor assemblyDescriptor) {
        this.assemblyDescriptor = assemblyDescriptor;
    }
}
