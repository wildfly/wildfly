/*
 * JBoss, Home of Professional Open Source. Copyright 2019, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.wildfly.extension.microprofile.openapi.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.microprofile.openapi._private.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer.Format;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.undertow.servlet.api.DeploymentInfo;

/**
 *
 * @author Michael Edgar
 *
 */
public class DeploymentProcessor implements DeploymentUnitProcessor {

    // Static files stored using a LinkedHashMap to maintain iteration order/priority
    private static final Map<String, Format> STATIC_FILES = new LinkedHashMap<>(6);

    static {
        STATIC_FILES.put("/META-INF/openapi.yaml", Format.YAML);
        STATIC_FILES.put("/WEB-INF/classes/META-INF/openapi.yaml", Format.YAML);
        STATIC_FILES.put("/META-INF/openapi.yml", Format.YAML);
        STATIC_FILES.put("/WEB-INF/classes/META-INF/openapi.yml", Format.YAML);
        STATIC_FILES.put("/META-INF/openapi.json", Format.JSON);
        STATIC_FILES.put("/WEB-INF/classes/META-INF/openapi.json", Format.JSON);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        final ServiceName deploymentServiceName = deploymentUnit.getServiceName();
        final ServiceName deploymentOpenAPIName = deploymentServiceName.append("openapi", "http-context");
        final ServiceName undertowServiceName = ServiceName.of("org","wildfly","undertow");
        final ServiceName deploymentInfoServiceName = UndertowService
                .deploymentServiceName(deploymentServiceName)
                .append(UndertowDeploymentInfoService.SERVICE_NAME);

        ServiceBuilder<?> builder = phaseContext.getServiceTarget().addService(deploymentOpenAPIName);
        Supplier<DeploymentInfo> deploymentInfoSupplier = builder.requires(deploymentInfoServiceName);
        Supplier<UndertowService> undertowSupplier = builder.requires(undertowServiceName);

        OpenAPIContextService service = new OpenAPIContextService(undertowSupplier, deploymentInfoSupplier);
        builder.setInstance(service).install();

        addListeners(deploymentUnit);
        loadOpenAPIModels(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        OpenApiDocument.INSTANCE.reset();
    }

    private void addListeners(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return;
        }

        ListenerMetaData listenerMetaData = new ListenerMetaData();
        listenerMetaData.setListenerClass(OpenApiServletContextListener.class.getName());

        List<ListenerMetaData> listeners = jbossWebMetaData.getListeners();
        if (null == listeners) {
            listeners = new ArrayList<>();
        }
        listeners.add(listenerMetaData);
        jbossWebMetaData.setListeners(listeners);
    }

    private JBossWebMetaData getJBossWebMetaData(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (null == warMetaData) {
            // not a web deployment, nothing to do here...
            return null;
        }

        return warMetaData.getMergedJBossWebMetaData();
    }

    private void loadOpenAPIModels(DeploymentUnit deploymentUnit) {
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        deploymentUnit.hasAttachment(AttachmentKey.create(Config.class));
        Config mpConfig = ConfigProvider.getConfig(module.getClassLoader());

        OpenApiConfig config = new OpenApiConfigImpl(mpConfig);
        IndexView index = getIndex(deploymentUnit, config);

        // Set models from annotations and static file
        OpenApiDocument openApiDocument = OpenApiDocument.INSTANCE;
        openApiDocument.config(config);
        moduleToStaticFile(module, openApiDocument);
        openApiDocument.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, index));
    }

    private IndexView getIndex(DeploymentUnit deploymentUnit, OpenApiConfig config) {
        Collection<Index> indexes = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes();
        CompositeIndex compositeIndex = CompositeIndex.create(indexes.stream().map(IndexView.class::cast).collect(Collectors.toList()));

        return new FilteredIndexView(compositeIndex, config);
    }

    /**
     * Finds the static OpenAPI file located in the deployment and, if it exists, adds it to the
     * provided OpenApiDocument.
     *
     * @param module Module containing the application
     * @param openApiDocument OpenApiDocument instance with which the static file will be merged
     */
    private void moduleToStaticFile(Module module, OpenApiDocument openApiDocument) {
        for (Map.Entry<String, Format> file : STATIC_FILES.entrySet()) {
            URL filePath = module.getExportedResource(file.getKey());

            if (filePath != null) {
                Format format = file.getValue();

                try (OpenApiStaticFile staticFile = new OpenApiStaticFile(filePath.openStream(), format)) {
                    openApiDocument.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(staticFile));
                    break;
                } catch (IOException e) {
                    MicroProfileOpenAPILogger.LOGGER.staticFileLoadException(e);
                }
            }
        }
    }
}
