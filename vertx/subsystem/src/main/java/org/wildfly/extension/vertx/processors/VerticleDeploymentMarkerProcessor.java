/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.processors;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX;
import static org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE;

/**
 *
 * Processor used to parse verticle deployments configuration from archive.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VerticleDeploymentMarkerProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.PARSE;
    public static final int PRIORITY = 0x4000;

    private static final DotName DOT_NAME_INJECTION = DotName.createSimple("jakarta.inject.Inject");
    private static final DotName DOT_NAME_VERTX_ANNOTATION = DotName.createSimple("io.vertx.core.Vertx");
    private static final DotName DOT_NAME_VERTX_MUTINY_ANNOTATION = DotName.createSimple("io.vertx.mutiny.core.Vertx");
    private static final DotName DOT_NAME_CDI_INSTANCE = DotName.createSimple("jakarta.enterprise.inject.Instance");

    private static final Set<DotName> VERTX_CLASSES = new HashSet<>();
    static {
        VERTX_CLASSES.add(DOT_NAME_VERTX_ANNOTATION);
        VERTX_CLASSES.add(DOT_NAME_VERTX_MUTINY_ANNOTATION);
    }

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(COMPOSITE_ANNOTATION_INDEX);
        if (annotated(index, DOT_NAME_INJECTION)) {
            VertxDeploymentAttachment.attachVertxDeployments(deploymentUnit);
        }
    }

    private boolean annotated(CompositeIndex index, DotName injectAnnotationName) {
        final List<AnnotationInstance> resourceAnnotations = index.getAnnotations(injectAnnotationName);
        for (AnnotationInstance annotation : resourceAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            if (annotationTarget instanceof FieldInfo) {
                Type type = annotationTarget.asField().type();
                final DotName fieldType = type.name();
                if (VERTX_CLASSES.contains(fieldType)) {
                    return true;
                }
                if (DOT_NAME_CDI_INSTANCE.equals(fieldType) && type.kind() == PARAMETERIZED_TYPE) {
                    List<Type> arguments = type.asParameterizedType().arguments();
                    if (arguments.size() == 1 && VERTX_CLASSES.contains(arguments.get(0).name())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        VertxDeploymentAttachment.removeVertxDeploymentsMeta(deploymentUnit);
    }

}
