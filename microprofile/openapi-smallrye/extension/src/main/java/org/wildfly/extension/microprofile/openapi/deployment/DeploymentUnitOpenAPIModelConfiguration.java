/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import io.smallrye.openapi.runtime.io.Format;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Encapsulates the {@link OpenAPIModelConfiguration} for a deployment.
 * @author Paul Ferraro
 */
public class DeploymentUnitOpenAPIModelConfiguration implements DeploymentOpenAPIModelConfiguration {

    private static final String RELATIVE_SERVER_URLS = "servers.relative";
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

    private final Config config;
    private final Optional<URL> staticFile;
    private final String serverName;
    private final String hostName;
    private final String modelName;
    private final Function<String, URL> resolver;

    DeploymentUnitOpenAPIModelConfiguration(DeploymentUnit unit) {
        this.config = ConfigProvider.getConfig(unit.getAttachment(Attachments.MODULE).getClassLoader());
        VirtualFile root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        this.resolver = new Function<>() {
            @Override
            public URL apply(String path) {
                try {
                    VirtualFile file = root.getChild(path);
                    return file.exists() ? file.toURL() : null;
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        this.staticFile = Stream.of(Format.YAML, Format.JSON).map(STATIC_FILES::get).flatMap(List::stream).map(this.resolver).filter(Objects::nonNull).findFirst();
        // Fetch server/host as determined by Undertow DUP
        ModelNode model = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        this.serverName = model.get(DeploymentDefinition.SERVER.getName()).asString();
        this.hostName = model.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();
        this.modelName = unit.getName();
    }

    @Override
    public Config getMicroProfileConfig() {
        return this.config;
    }

    @Override
    public Optional<URL> getStaticFile() {
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
    public String getModelName() {
        return this.modelName;
    }

    @Override
    public Function<String, URL> getResourceResolver() {
        return this.resolver;
    }

    @Override
    public boolean useRelativeServerURLs() {
        return this.config.getOptionalValue(OASConfig.EXTENSIONS_PREFIX + RELATIVE_SERVER_URLS, Boolean.class).orElse(Boolean.TRUE);
    }
}