/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * Represents an @Resource injection that has no source defined. If a binding for the corresponding
 * name is defined using an env-entry in a deployment descriptor then these resource injections are installed.
 * <p/>
 * Otherwise they are ignored.
 *
 * TODO: this is fairly horrible, if anyone can think of a better way of doing this i'm all ears
 *
 * @author Stuart Douglas
 */
public class LazyResourceInjection {

    private final EEModuleClassDescription classDescription;
    private final InjectionTarget injectionTarget;
    private final String localContextName;
    private boolean installed = false;

    public LazyResourceInjection( final InjectionTarget injectionTarget, final String localContextName, final EEModuleClassDescription classDescription) {
        this.injectionTarget = injectionTarget;
        this.localContextName = localContextName;
        this.classDescription = classDescription;
    }

    public void install() {
        if(!installed) {
            final ResourceInjectionConfiguration resource = new ResourceInjectionConfiguration(injectionTarget, new LookupInjectionSource(localContextName));
            classDescription.getConfigurators().add(new InjectionConfigrator(resource));
            installed = true;
        }
    }

    public String getLocalContextName() {
        return localContextName;
    }

    private static class InjectionConfigrator implements ClassConfigurator {

        private final ResourceInjectionConfiguration injectionConfiguration;

        public InjectionConfigrator(final ResourceInjectionConfiguration injectionConfiguration) {
            this.injectionConfiguration = injectionConfiguration;
        }


        @Override
        public void configure(final DeploymentPhaseContext context, final EEModuleClassDescription description, final EEModuleClassConfiguration configuration) throws DeploymentUnitProcessingException {
            configuration.getInjectionConfigurations().add(injectionConfiguration);
        }
    }
}
