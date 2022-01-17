/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * Requirement definition for EJB abstractions.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum EjbProviderRequirement implements DefaultableUnaryRequirement, DefaultableUnaryServiceNameFactoryProvider {
    BEAN_MANAGEMENT_PROVIDER("org.wildfly.clustering.ejb.bean-management-provider", EjbDefaultProviderRequirement.BEAN_MANAGEMENT_PROVIDER),
    ;
    private final String name;
    private final UnaryServiceNameFactory factory;
    private final EjbDefaultProviderRequirement defaultRequirement;

    EjbProviderRequirement(String name, EjbDefaultProviderRequirement defaultRequirement) {
        this.name = name;
        this.factory = new UnaryRequirementServiceNameFactory(this);
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public ServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }

    @Override
    public Requirement getDefaultRequirement() {
        return this.defaultRequirement;
    }
}
