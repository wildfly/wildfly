/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.jboss.arquillian.api.ArchiveDeployer;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.TestEnricher;
import org.jboss.as.arquillian.common.ArchiveDeployerImpl;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;

/**
 * The {@link ArchiveDeployer} TestEnricher
 *
 * The enricher supports the injection of the {@link ArchiveDeployer}.
 *
 * <pre>
 * <code>
 *    @Inject
 *    ArchiveDeployer deployer;
 * </code>
 * </pre>
 *
 * @author thomas.diesler@jboss.com
 * @since 17-Feb-2011
 */
public class ArchiveDeployerTestEnricher implements TestEnricher {

    private ServerDeploymentManager deploymentManager;

    public void inject(ServerDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    @Override
    public void enrich(Context context, Object testCase) {

        // [TODO] Remove this hacks when it becomes possible to pass data to the enrichers
        inject(ServerDeploymentManagerAssociation.getServerDeploymentManager());

        Class<?> testClass = testCase.getClass();
        for (Field field : testClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (field.getType().isAssignableFrom(ArchiveDeployer.class)) {
                    injectArchiveDeployer(context, testCase, field);
                }
            }
        }
    }

    @Override
    public Object[] resolve(Context context, Method method) {
        return null;
    }

    private void injectArchiveDeployer(Context context, Object testCase, Field field) {
        try {
            field.set(testCase, new ArchiveDeployerImpl(deploymentManager));
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot inject DeploymentProvider", ex);
        }
    }
}
