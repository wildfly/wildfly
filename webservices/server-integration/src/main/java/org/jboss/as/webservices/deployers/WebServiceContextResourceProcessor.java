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

package org.jboss.as.webservices.deployers;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ClassConfigurator;
import org.jboss.as.ee.component.EEModuleClassConfiguration;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.ResourceInjectionAnnotationParsingProcessor;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.webservices.injection.WebServiceContextInjectionSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import javax.xml.ws.WebServiceContext;

/**
 * Processes {@link javax.annotation.Resource @Resource} and {@link javax.annotation.Resources @Resources} annotations
 * for a {@link WebServiceContext} type resource
 * <p/>
 * User : Jaikiran Pai
 */
public class WebServiceContextResourceProcessor extends ResourceInjectionAnnotationParsingProcessor {

    private static final Logger logger = Logger.getLogger(WebServiceContextResourceProcessor.class);

    @Override
    protected void process(EEModuleClassDescription classDescription, AnnotationInstance annotation, String injectionType, String localContextName, InjectionTarget targetDescription, EEModuleDescription eeModuleDescription) {
        // we process only @Resource of type WebServiceContext
        if (!WebServiceContext.class.getName().equals(injectionType)) {
            return;
        }
        logger.debug("Processing @Resource of type: " + WebServiceContext.class.getName() + " for ENC name: " + localContextName);
        // setup the ENC binding
        final InjectionSource bindingSource = new WebServiceContextInjectionSource();
        final BindingConfiguration bindingConfiguration = new BindingConfiguration(localContextName, bindingSource);

        // setup the injection target (if any)
        // our injection comes from the local lookup, no matter what.
        final InjectionSource injectionSource = new LookupInjectionSource(localContextName);
        final ResourceInjectionConfiguration resourceInjectionConfiguration = targetDescription != null ? new ResourceInjectionConfiguration(targetDescription, injectionSource) : null;

        // add the binding and injection configurator
        classDescription.getConfigurators().add(new ClassConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, EEModuleClassDescription description, EEModuleClassConfiguration configuration) throws DeploymentUnitProcessingException {
                // add the ENC binding configuration
                configuration.getBindingConfigurations().add(bindingConfiguration);
                // add the injection configuration (if any)
                if (resourceInjectionConfiguration != null) {
                    configuration.getInjectionConfigurations().add(resourceInjectionConfiguration);
                }
            }
        });

    }
}
