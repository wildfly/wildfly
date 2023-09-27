/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.weld;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.jandex.DotName;

/**
 * @author Stuart Douglas
 */
public class InjectionTargetDefiningAnnotations {

    /**
     * A set of injection target defining annotations. These are annotations that are not enough to cause weld to activate,
     * however if weld is activated these will be turned into beans.
     */
    public static final AttachmentKey<AttachmentList<DotName>> INJECTION_TARGET_DEFINING_ANNOTATIONS = AttachmentKey.createList(DotName.class);

    private InjectionTargetDefiningAnnotations() {

    }

}
