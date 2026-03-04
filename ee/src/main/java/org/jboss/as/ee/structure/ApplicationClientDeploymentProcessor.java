/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Processor that sets up application clients. If this is an ear deployment it will mark the client as
 * a sub deployment.
 * If this is an application client deployment it will set the deployment type. If the client deployment
 * is a subdeployment, it will mark the deployment module as private so it is not made visible to other subdeployment modules.
 * <p/>
 *
 * @author Stuart Douglas
 */
public class ApplicationClientDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String META_INF_APPLICATION_CLIENT_XML = "META-INF/application-client.xml";

    /**
     * Allowing appclient jar modules to be visible to other subdeployments in an ear is contrary
     * to various EE 10 section 8.3 requirements, but WildFly did it for many years, so allow users
     * who rely on that behavior to have a workaround to restore it by setting a system property to 'false'
     */
    private static final boolean APP_CLIENT_ISOLATED =
            Boolean.parseBoolean(System.getProperty("wildfly.appclient.subdeployment.isolated", "true"));

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            List<ResourceRoot> potentialSubDeployments = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
            for (ResourceRoot resourceRoot : potentialSubDeployments) {
                if (ModuleRootMarker.isModuleRoot(resourceRoot)) {
                    // module roots cannot be ejb jars
                    continue;
                }
                VirtualFile appclientClientXml = resourceRoot.getRoot().getChild(META_INF_APPLICATION_CLIENT_XML);
                if (appclientClientXml.exists()) {
                    SubDeploymentMarker.mark(resourceRoot);
                    ModuleRootMarker.mark(resourceRoot);
                } else {
                    final Manifest manifest = getManifest(resourceRoot);
                    if (manifest != null) {
                        Attributes main = manifest.getMainAttributes();
                        if (main != null) {
                            String mainClass = main.getValue("Main-Class");
                            if (mainClass != null && !mainClass.isEmpty()) {
                                SubDeploymentMarker.mark(resourceRoot);
                                ModuleRootMarker.mark(resourceRoot);
                            }
                        }
                    }
                }
            }
        } else if (deploymentUnit.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
            final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final ModuleMetaData md = root.getAttachment(org.jboss.as.ee.structure.Attachments.MODULE_META_DATA);
            if (md != null) {
                if (md.getType() == ModuleMetaData.ModuleType.Client) {
                    DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
                }
            } else {
                VirtualFile appclientClientXml = root.getRoot().getChild(META_INF_APPLICATION_CLIENT_XML);
                if (appclientClientXml.exists()) {
                    DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
                } else {
                    final Manifest manifest = root.getAttachment(Attachments.MANIFEST);
                    if (manifest != null) {
                        Attributes main = manifest.getMainAttributes();
                        if (main != null) {
                            String mainClass = main.getValue("Main-Class");
                            if (mainClass != null && !mainClass.isEmpty()) {
                                DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
                            }
                        }
                    }
                }
            }

            if (APP_CLIENT_ISOLATED && deploymentUnit.getParent() != null
                    // limit this to non-appclient processes
                    // See 'Components in the application client container must have access to the following classes and resources.' discussion in:
                    // https://jakarta.ee/specifications/platform/10/jakarta-platform-spec-10.0#application-client-container-class-loading-requirements
                    && deploymentUnit.getParent().getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION) != null
                    && !deploymentUnit.getParent().getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION).isAppClient()
                    && DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
                // Don't allow other ear submodules to see application client archives
                // See 'must not have access' discussions in:
                // https://jakarta.ee/specifications/platform/10/jakarta-platform-spec-10.0#a3046
                // https://jakarta.ee/specifications/platform/10/jakarta-platform-spec-10.0#jakarta-enterprise-beans-container-class-loading-requirements
                // https://jakarta.ee/specifications/platform/10/jakarta-platform-spec-10.0#application-client-container-class-loading-requirements
                final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
                if (moduleSpecification != null) {
                    moduleSpecification.setPrivateModule(true);
                } // TODO WarStructureDeploymentProcessor accounts for the null case so this does too but it's not clear why it would legitimately be null
            }
        }
    }

    private static Manifest getManifest(ResourceRoot resourceRoot) throws DeploymentUnitProcessingException {
        Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
        if (manifest == null) {
            // This is expected as ManifestAttachmentProcessor attaches when processing the subdeployment,
            // but this DUP runs on the top level deployment before subdeployment processing starts.
            // So find the manifest ourselves.
            final VirtualFile deploymentRoot = resourceRoot.getRoot();
            try {
                manifest = VFSUtils.getManifest(deploymentRoot);
            } catch (IOException e) {
                throw ServerLogger.ROOT_LOGGER.failedToGetManifest(deploymentRoot, e);
            }
        }
        return manifest;
    }
}
