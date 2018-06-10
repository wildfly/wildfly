/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jpa.hibernate5.service;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Contribute specialized Hibernate Service impls
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 */
public class ServiceContributorImpl implements ServiceContributor {
    private static final String CONTROLJTAINTEGRATION = "wildfly.jpa.jtaplatform"; // these properties are documented in org.jboss.as.jpa.config.Configuration
    private static final String CONTROL2LCINTEGRATION = "wildfly.jpa.regionfactory";

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        // note that the following deprecated getSettings() is agreed to be replaced with method that returns immutable copy of configuration settings.
        final Object jtaPlatformInitiatorEnabled = serviceRegistryBuilder.getSettings().getOrDefault(CONTROLJTAINTEGRATION, true);

        if (jtaPlatformInitiatorEnabled == null ||
                (jtaPlatformInitiatorEnabled instanceof Boolean && ((Boolean) jtaPlatformInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(jtaPlatformInitiatorEnabled.toString())) {
            serviceRegistryBuilder.addInitiator(new WildFlyCustomJtaPlatformInitiator());
        }

        final Object regionFactoryInitiatorEnabled = serviceRegistryBuilder.getSettings().getOrDefault(CONTROL2LCINTEGRATION, true);

        if (regionFactoryInitiatorEnabled == null ||
                (regionFactoryInitiatorEnabled instanceof Boolean && ((Boolean) regionFactoryInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(regionFactoryInitiatorEnabled.toString())) {
            serviceRegistryBuilder.addInitiator(new WildFlyCustomRegionFactoryInitiator());
        }
    }
}
