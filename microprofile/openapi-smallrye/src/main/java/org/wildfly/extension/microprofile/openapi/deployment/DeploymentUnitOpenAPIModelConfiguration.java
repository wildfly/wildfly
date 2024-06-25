/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.io.Format;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Encapsulates the {@link OpenAPIModelConfiguration} for a deployment.
 * @author Paul Ferraro
 */
public class DeploymentUnitOpenAPIModelConfiguration implements OpenAPIModelConfiguration {

    private static final String ENABLED = "mp.openapi.extensions.enabled";
    private static final String PATH = "mp.openapi.extensions.path";
    private static final String DEFAULT_PATH = "/openapi";
    private static final String RELATIVE_SERVER_URLS = "mp.openapi.extensions.servers.relative";
    private static final Map<Format, List<String>> STATIC_FILES = new EnumMap<>(Format.class);
    static {
        // Order resource names by search order
        STATIC_FILES.put(Format.YAML, List.of(
                "/META-INF/openapi.yaml",
                "/WEB-INF/classes/META-INF/openapi.yaml",
                "/META-INF/openapi.yml",
                "/WEB-INF/classes/META-INF/openapi.yml"));
        STATIC_FILES.put(Format.JSON, List.of(
                "/META-INF/openapi.json",
                "/WEB-INF/classes/META-INF/openapi.json"));
    }

    private static Map.Entry<VirtualFile, Format> findStaticFile(VirtualFile root) {
        // Format search order
        for (Format format : EnumSet.of(Format.YAML, Format.JSON)) {
            for (String resource : STATIC_FILES.get(format)) {
                VirtualFile file = root.getChild(resource);
                if (file.exists()) {
                    return Map.entry(file, format);
                }
            }
        }
        return null;
    }

    private final boolean enabled;
    private final OpenApiConfig openApiConfig;
    private final Map.Entry<VirtualFile, Format> staticFile;
    private final String serverName;
    private final String hostName;
    private final String path;
    private final boolean relativeServerURLs;

    DeploymentUnitOpenAPIModelConfiguration(DeploymentUnit unit) {
        Config config = ConfigProvider.getConfig(unit.getAttachment(Attachments.MODULE).getClassLoader());
        this.enabled = config.getOptionalValue(ENABLED, Boolean.class).orElse(Boolean.TRUE);
        this.openApiConfig = OpenApiConfig.fromConfig(config);
        this.staticFile = findStaticFile(unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot());
        // Fetch server/host as determined by Undertow DUP
        ModelNode model = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        this.serverName = model.get(DeploymentDefinition.SERVER.getName()).asString();
        this.hostName = model.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();
        this.path = config.getOptionalValue(PATH, String.class).orElse(DEFAULT_PATH);
        if (!this.path.equals(DEFAULT_PATH)) {
            MicroProfileOpenAPILogger.LOGGER.nonStandardEndpoint(unit.getName(), this.path, DEFAULT_PATH);
        }
        this.relativeServerURLs = config.getOptionalValue(RELATIVE_SERVER_URLS, Boolean.class).orElse(Boolean.TRUE);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public OpenApiConfig getOpenApiConfig() {
        return this.openApiConfig;
    }

    @Override
    public Map.Entry<VirtualFile, Format> getStaticFile() {
        return this.staticFile;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public String getHostName() {
        return this.hostName;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean useRelativeServerURLs() {
        return this.relativeServerURLs;
    }
}