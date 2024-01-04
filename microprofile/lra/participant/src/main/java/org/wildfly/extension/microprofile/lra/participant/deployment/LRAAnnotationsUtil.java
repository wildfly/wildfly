/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant.deployment;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.Leave;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.DotName;

public class LRAAnnotationsUtil {

    private static final DotName[] LRA_ANNOTATIONS = {
        DotName.createSimple(LRA.class),
        DotName.createSimple(Complete.class),
        DotName.createSimple(Compensate.class),
        DotName.createSimple(Status.class),
        DotName.createSimple(Forget.class),
        DotName.createSimple(Leave.class),
        DotName.createSimple(AfterLRA.class)
    };


    public static boolean isNotLRADeployment(DeploymentUnit deploymentUnit) {
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return true;
        }

        return !isLRAAnnotationsPresent(compositeIndex);
    }

    private static boolean isLRAAnnotationsPresent(CompositeIndex compositeIndex) {
        for (DotName annotation : LRA_ANNOTATIONS) {
            if (compositeIndex.getAnnotations(annotation).size() > 0) {
                return true;
            }
        }
        return false;
    }
}
