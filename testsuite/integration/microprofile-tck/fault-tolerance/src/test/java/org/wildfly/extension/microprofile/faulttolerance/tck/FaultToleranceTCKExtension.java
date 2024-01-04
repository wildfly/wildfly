/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.faulttolerance.tck;

import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * @author Radoslav Husar
 */
public class FaultToleranceTCKExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeploymentExceptionTransformer.class, WildFlyDeploymentExceptionTransformer.class);
        builder.service(ApplicationArchiveProcessor.class, FaultToleranceApplicationArchiveProcessor.class);
    }

}