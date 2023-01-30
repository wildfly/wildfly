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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * The meta-data of the verticle deployment.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VerticleDeploymentsMetaData {

    public static final String DEPLOYMENT_CONFIG_VERSION = "ver";
    public static final String DEPLOYMENTS = "deployments";
    private static final int DEFAULT_VERSION = 1;

    public static final String DEFAULT_VERTX_NAME = "default";
    public static final String VERTICLE_CLASS = "verticle-class";
    public static final String VERTX_NAME = "vertx";
    public static final String DEPLOY_OPTIONS = "deploy-options";

    private final List<JsonObject> deployments;

    public VerticleDeploymentsMetaData(JsonObject json) {
        final int version = json.getInteger(DEPLOYMENT_CONFIG_VERSION, DEFAULT_VERSION);
        if (version != 1) {
            // currently version needs only to be 1
            throw new IllegalArgumentException("Not supported json schema version " + version);
        }
        this.deployments = json.getJsonArray(DEPLOYMENTS, new JsonArray()).stream()
                .map(o -> (JsonObject)o).collect(Collectors.toList());
    }

    /**
     * Gets the list of verticle deployment configurations.
     *
     * @return the list of verticle deployment configurations.
     */
    public List<JsonObject> getVerticleDeployments() {
        return this.deployments;
    }

}
