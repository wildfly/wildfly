/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.faulttolerance.tck;

import java.util.Objects;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;

/**
 * Temporary workaround for WFARQ-59 where the exception is an instance of DeploymentException
 * but its cause is null.
 *
 * @author Radoslav Husar
 */
public class WildFlyDeploymentExceptionTransformer implements DeploymentExceptionTransformer {

    @Override
    public Throwable transform(Throwable throwable) {
        return Objects.requireNonNullElseGet(throwable, FaultToleranceDefinitionException::new);
    }

}