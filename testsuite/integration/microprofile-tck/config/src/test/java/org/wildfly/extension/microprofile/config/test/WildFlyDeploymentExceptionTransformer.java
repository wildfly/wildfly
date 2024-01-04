/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.config.test;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
public class WildFlyDeploymentExceptionTransformer implements DeploymentExceptionTransformer {

    public Throwable transform(Throwable throwable) {
        // Due to https://issues.redhat.com/browse/WFARQ-59 if the deployment fails, WildFly Arquillian
        // returns a DeploymentException without a cause. In that case (throwable == null), we create a new DeploymentException
        // so that the test will properly get a DeploymentException (the actual cause of the deployment failure is lost though).
        if (throwable == null) {
            return new DeploymentException("Deployment on WildFly was unsuccessful. Look at the WildFly server logs to have more information on the actual cause of the deployment failure");
        }
        return throwable;
    }

}