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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX;

/**
 *
 * Processor used to parse verticle deployments configuration from archive.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VerticleDeploymentMarkerProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.PARSE;
    public static final int PRIORITY = 0x4000;

    private static final String VERTX_ANNOTATION_NAME = "io.vertx.core.Vertx";
    private static final String VERTX_MUTINY_ANNOTATION_NAME = "io.vertx.mutiny.core.Vertx";

    private static final List<DotName> dotNames = new ArrayList<>();
    private static final Set<String> VERTX_CLASSES = new HashSet<>();
    static {
        dotNames.add(DotName.createSimple("jakarta.inject.Inject"));
        dotNames.add(DotName.createSimple("jakarta.annotation.Resource"));
        VERTX_CLASSES.add(VERTX_ANNOTATION_NAME);
        VERTX_CLASSES.add(VERTX_MUTINY_ANNOTATION_NAME);
    }

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(COMPOSITE_ANNOTATION_INDEX);
        if (dotNames.stream().anyMatch(dotName -> annotated(index, dotName))) {
            VertxDeploymentAttachment.attachVertxDeployments(deploymentUnit);
        }
    }

    private boolean annotated(CompositeIndex index, DotName injectAnnotationName) {
        final List<AnnotationInstance> resourceAnnotations = index.getAnnotations(injectAnnotationName);
        for (AnnotationInstance annotation : resourceAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            if (annotationTarget instanceof FieldInfo) {
                final String fieldType = annotationTarget.asField().type().name().toString();
                if (VERTX_CLASSES.contains(fieldType)) {
                    return true;
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
