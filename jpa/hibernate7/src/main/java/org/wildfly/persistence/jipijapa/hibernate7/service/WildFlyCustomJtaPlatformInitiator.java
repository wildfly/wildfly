/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7.service;

import java.util.Map;

import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Custom JtaPlatform initiator for use inside WildFly picking an appropriate
 * fallback JtaPlatform.
 *
 * @author Steve Ebersole
 */
public class WildFlyCustomJtaPlatformInitiator extends JtaPlatformInitiator {
    @Override
    public JtaPlatform initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new WildFlyCustomJtaPlatform();
    }
}
