/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.AbstractComponentConfigProcessor;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.timerservice.TimerServiceBindingSource;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;

/**
 * Deployment processor responsible for detecting EJB components and adding a {@link BindingConfiguration} for the
 * java:comp/TimerService entry.
 * <p/>
 *
 * User: Jaikiran Pai
 */
public class TimerServiceJndiBindingProcessor extends AbstractComponentConfigProcessor {

    @Override
    protected void processComponentConfig(DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext, CompositeIndex index, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        if (!(componentDescription instanceof EJBComponentDescription)) {
            return;  // Only process EJBs
        }
        // if the EJB is packaged in a .war, then we need to bind the java:comp/TimerService only once for the entire module
        if (componentDescription.getNamingMode() != ComponentNamingMode.CREATE) {
            // get the module description
            final EEModuleDescription moduleDescription = componentDescription.getModuleDescription();
            // the java:module/TimerService binding configuration
            // Note that we bind to java:module/TimerService since it's a .war. End users can still lookup java:comp/TimerService
            // and that will internally get translated to  java:module/TimerService for .war, since java:comp == java:module in
            // a web ENC. So binding to java:module/TimerService is OK.
            final BindingConfiguration timerServiceBinding = new BindingConfiguration("java:module/TimerService", new TimerServiceBindingSource());
            moduleDescription.getBindingConfigurations().add(timerServiceBinding);
        } else { // EJB packaged outside of a .war. So process normally.
            // add the binding configuration to the component description
            final BindingConfiguration timerServiceBinding = new BindingConfiguration("java:comp/TimerService", new TimerServiceBindingSource());
            componentDescription.getBindingConfigurations().add(timerServiceBinding);
        }
    }
}
