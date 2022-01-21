/*
 * Copyright (C) 2022 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx.deployment;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.vertx.logging.VertxLogger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.TimeUnit;

/**
 * The Verticle deployment service that corresponds to a verticle deployment within the deployment unit.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VerticleDeploymentService implements Service {

    private static final long DEFAULT_DEPLOY_TIME_OUT = 30L;

    private final String verticleClass;
    private final String vertxJndiName;
    private final DeploymentOptions deploymentOptions;
    private final ClassLoader classLoader;

    private Vertx vertx;
    private String deploymentID;

    public VerticleDeploymentService(String verticleClass, String vertxJndiName, DeploymentOptions deploymentOptions,
                                     ClassLoader classLoader) {
        this.verticleClass = verticleClass;
        this.vertxJndiName = vertxJndiName;
        this.deploymentOptions = deploymentOptions;
        this.classLoader = classLoader;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        if (this.vertx == null) {
            try {
                this.vertx = lookupVertx();
            } catch (NamingException e) {
                throw VertxLogger.VERTX_LOGGER.failedToLookupVertx(this.vertxJndiName, e);
            }
        }
        try {
            this.deploymentOptions.setClassLoader(classLoader);
            Class<? extends Verticle> vcls = (Class<? extends Verticle>)classLoader.loadClass(verticleClass);
            this.deploymentID = this.vertx.deployVerticle(vcls, this.deploymentOptions)
                    .toCompletionStage().toCompletableFuture().get(DEFAULT_DEPLOY_TIME_OUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw VertxLogger.VERTX_LOGGER.failedToDeployVerticle(this.verticleClass, this.vertxJndiName, e);
        }
    }

    private Vertx lookupVertx() throws NamingException {
        return (Vertx)new InitialContext().lookup(this.vertxJndiName);
    }

    @Override
    public void stop(StopContext stopContext) {
        if (this.vertx != null && this.deploymentID != null) {
            try {
                this.vertx.undeploy(this.deploymentID).toCompletionStage()
                        .toCompletableFuture().get(DEFAULT_DEPLOY_TIME_OUT, TimeUnit.SECONDS);
                this.vertx = null;
                this.deploymentID = null;
            } catch (Exception e) {
                VertxLogger.VERTX_LOGGER.errorWhenUndeployVerticle(this.verticleClass, this.vertxJndiName, e);
            }
        }
    }
}
