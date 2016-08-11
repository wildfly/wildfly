package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.util.Utils.getRootDeploymentUnit;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.spi.ImplicitBeanArchiveDetector;
import org.jboss.as.weld.util.Utils;
import org.jboss.jandex.Index;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deployment processor that finds implicit bean archives (as defined by the CDI spec). If the deployment unit contains any such
 * archive, the deployment unit is marked using {@link WeldDeploymentMarker}.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldImplicitDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            return;
        }
        if (Utils.getRootDeploymentUnit(deploymentUnit).getAttachment(WeldConfiguration.ATTACHMENT_KEY).isRequireBeanDescriptor()) {
            // if running in the require-bean-descriptor mode then bean archives are found by BeansXmlProcessor
            return;
        }

        /*
         * look for classes with bean defining annotations
         */
        final Set<AnnotationType> beanDefiningAnnotations = new HashSet<>(getRootDeploymentUnit(deploymentUnit).getAttachment(WeldAttachments.BEAN_DEFINING_ANNOTATIONS));

        final Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
        final ExplicitBeanArchiveMetadataContainer explicitBeanArchiveMetadata = deploymentUnit.getAttachment(ExplicitBeanArchiveMetadataContainer.ATTACHMENT_KEY);
        final ResourceRoot classesRoot = deploymentUnit.getAttachment(WeldAttachments.CLASSES_RESOURCE_ROOT);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        for (Entry<ResourceRoot, Index> entry : indexes.entrySet()) {
            ResourceRoot resourceRoot = entry.getKey();
            if (resourceRoot == classesRoot) {
                // BDA for WEB-INF/classes is keyed under deploymentRoot in explicitBeanArchiveMetadata
                resourceRoot = deploymentRoot;
            }
            /*
             * Make sure bean defining annotations used in archives with bean-discovery-mode="none" are not considered here
             * WFLY-4388
             */
            if (explicitBeanArchiveMetadata != null && explicitBeanArchiveMetadata.getBeanArchiveMetadata().containsKey(resourceRoot)) {
                continue;
            }
            for (final AnnotationType annotation : beanDefiningAnnotations) {
                if (!entry.getValue().getAnnotations(annotation.getName()).isEmpty()) {
                    WeldDeploymentMarker.mark(deploymentUnit);
                    return;
                }
            }
        }

        /*
         * look for session beans and managed beans
         */
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final Iterable<ImplicitBeanArchiveDetector> detectors = ServiceLoader.load(ImplicitBeanArchiveDetector.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldImplicitDeploymentProcessor.class));

        for (ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
            for (ImplicitBeanArchiveDetector detector : detectors) {
                if (detector.isImplicitBeanArchiveRequired(component)) {
                    WeldDeploymentMarker.mark(deploymentUnit);
                    return;
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
