/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.arquillian.generic.service;

import java.util.Set;

import org.jboss.as.arquillian.service.ArquillianService;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class GenericArquillianService extends ArquillianService<GenericArquillianConfig> implements Service<GenericArquillianService> {
    public static void addService(final ServiceTarget serviceTarget) {
        GenericArquillianService service = new GenericArquillianService();
        ServiceBuilder<GenericArquillianService> serviceBuilder = serviceTarget.addService(ArquillianService.SERVICE_NAME, service);
        service.build(serviceBuilder);
        serviceBuilder.install();
    }

    @Override
    protected GenericArquillianConfig createArquillianConfig(DeploymentUnit depUnit, Set<String> testClasses) {
        return new GenericArquillianConfig(this, depUnit, testClasses);
    }

    @Override
    public GenericArquillianService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    protected void startingToUp(ServiceBuilder serviceBuilder, GenericArquillianConfig arqConfig) {
        // no-op
    }
}
