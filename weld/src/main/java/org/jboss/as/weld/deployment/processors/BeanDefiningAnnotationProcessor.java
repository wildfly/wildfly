package org.jboss.as.weld.deployment.processors;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static org.jboss.as.weld.deployment.WeldAttachments.BEAN_DEFINING_ANNOTATIONS;
import static org.jboss.as.weld.discovery.AnnotationType.FOR_CLASSINFO;
import static org.jboss.as.weld.util.Indices.ANNOTATION_PREDICATE;
import static org.jboss.as.weld.util.Indices.getAnnotatedClasses;

import java.util.Collection;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.CdiAnnotations;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.jandex.DotName;

import com.google.common.collect.ImmutableSet;

/**
 * Determines the set of bean defining annotations as defined by the CDI specification and attaches them under
 * {@link WeldAttachments.#BEAN_DEFINING_ANNOTATIONS}.
 *
 * @author Jozef Hartinger
 *
 */
public class BeanDefiningAnnotationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return; // only run for top-level deployments
        }

        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        ImmutableSet.Builder<AnnotationType> beanDefiningAnnotations = ImmutableSet.builder();
        beanDefiningAnnotations.addAll(CdiAnnotations.BUILT_IN_SCOPES);
        beanDefiningAnnotations.addAll(getAnnotationsAnnotatedWith(index, CdiAnnotations.NORM_SCOPE.getDotName()));
        beanDefiningAnnotations.addAll(getAnnotationsAnnotatedWith(index, CdiAnnotations.SCOPE));

        deploymentUnit.putAttachment(BEAN_DEFINING_ANNOTATIONS, beanDefiningAnnotations.build());
    }

    private Collection<AnnotationType> getAnnotationsAnnotatedWith(CompositeIndex index, DotName annotationName) {
        return transform(filter(getAnnotatedClasses(index.getAnnotations(annotationName)), ANNOTATION_PREDICATE), FOR_CLASSINFO);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
