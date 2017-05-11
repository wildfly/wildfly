package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.discovery.AnnotationType.FOR_CLASSINFO;
import static org.jboss.as.weld.util.Indices.ANNOTATION_PREDICATE;
import static org.jboss.as.weld.util.Indices.getAnnotatedClasses;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.TransactionScoped;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.CdiAnnotations;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;


/**
 * Determines the set of bean defining annotations as defined by the CDI specification and attaches them under
 * {@link WeldAttachments#BEAN_DEFINING_ANNOTATIONS}.
 *
 * @author Jozef Hartinger
 *
 */
public class BeanDefiningAnnotationProcessor implements DeploymentUnitProcessor {

    private static final DotName VIEW_SCOPED_NAME = DotName.createSimple("javax.faces.view.ViewScoped");
    private static final DotName FLOW_SCOPED_NAME = DotName.createSimple("javax.faces.flow.FlowScoped");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return; // only run for top-level deployments
        }

        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        // CDI built-in normal scopes plus @Dependent
        addAnnotations(deploymentUnit, CdiAnnotations.BEAN_DEFINING_ANNOTATIONS);
        // CDI @Model stereotype
        addAnnotation(deploymentUnit, new AnnotationType(CdiAnnotations.MODEL.getDotName(), false));
        // EE7 built-in normal scopes and stereotypes
        addAnnotation(deploymentUnit, new AnnotationType(TransactionScoped.class));
        addAnnotation(deploymentUnit, new AnnotationType(VIEW_SCOPED_NAME, true));
        addAnnotation(deploymentUnit, new AnnotationType(FLOW_SCOPED_NAME, true));

        for (AnnotationType annotationType : CdiAnnotations.BEAN_DEFINING_META_ANNOTATIONS) {
            addAnnotations(deploymentUnit, getAnnotationsAnnotatedWith(index, annotationType.getName()));
        }
    }

    private static void addAnnotations(final DeploymentUnit deploymentUnit, Collection<AnnotationType> annotations) {
        for(AnnotationType annotation : annotations){
            addAnnotation(deploymentUnit, annotation);
        }
    }

    private static void addAnnotation(final DeploymentUnit deploymentUnit, AnnotationType annotation) {
        deploymentUnit.addToAttachmentList(WeldAttachments.BEAN_DEFINING_ANNOTATIONS, annotation);
    }

    private Collection<AnnotationType> getAnnotationsAnnotatedWith(CompositeIndex index, DotName annotationName) {
        Set<AnnotationType> annotations = new HashSet<>();
        for (ClassInfo classInfo : getAnnotatedClasses(index.getAnnotations(annotationName))) {
            if (ANNOTATION_PREDICATE.test(classInfo)) {
                annotations.add(FOR_CLASSINFO.apply(classInfo));
            }
        }
        return annotations;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(WeldAttachments.BEAN_DEFINING_ANNOTATIONS);
    }
}
