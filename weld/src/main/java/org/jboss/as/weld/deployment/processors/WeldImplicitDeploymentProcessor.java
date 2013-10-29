package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.util.Utils.getRootDeploymentUnit;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.managedbean.component.ManagedBeanComponentDescription;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.util.Utils;

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
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final Set<AnnotationType> beanDefiningAnnotations = new HashSet<>(getRootDeploymentUnit(deploymentUnit).getAttachment(WeldAttachments.BEAN_DEFINING_ANNOTATIONS));

        for (final AnnotationType annotation : beanDefiningAnnotations) {
            if (!index.getAnnotations(annotation.getName()).isEmpty()) {
                WeldDeploymentMarker.mark(deploymentUnit);
                return;
            }
        }

        /*
         * look for session beans and managed beans
         */
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        for (ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
            if (component instanceof SessionBeanComponentDescription || component instanceof ManagedBeanComponentDescription) {
                WeldDeploymentMarker.mark(deploymentUnit);
                return;
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
