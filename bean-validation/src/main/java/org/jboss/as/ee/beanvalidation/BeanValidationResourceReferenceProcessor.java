/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.beanvalidation;

import javax.validation.Validator;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * Handled resource injections for the Validator
 *
 * @author Stuart Douglas
 */
public class BeanValidationResourceReferenceProcessor implements EEResourceReferenceProcessor {

    public static final BeanValidationResourceReferenceProcessor INSTANCE = new BeanValidationResourceReferenceProcessor();

    @Override
    public String getResourceReferenceType() {
        return Validator.class.getName();
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        return ValidatorInjectionSource.INSTANCE;
    }

    private static final class ValidatorInjectionSource extends InjectionSource {

        public static final ValidatorInjectionSource INSTANCE = new ValidatorInjectionSource();

        @Override
        public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
            final ClassLoader classLoader = phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE).getClassLoader();
            injector.inject(new ValidatorJndiInjectable(new LazyValidatorFactory(classLoader)));
        }
    }
}
