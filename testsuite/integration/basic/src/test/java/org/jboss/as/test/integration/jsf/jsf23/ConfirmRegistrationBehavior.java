/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.jsf23;

import jakarta.faces.component.behavior.ClientBehaviorBase;
import jakarta.faces.component.behavior.ClientBehaviorContext;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.inject.Inject;

/**
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@FacesBehavior(value = "confirm", managed = true)
public class ConfirmRegistrationBehavior extends ClientBehaviorBase {

    @Inject
    ConfirmBean confirmBean;

    @Override
    public String getScript(ClientBehaviorContext behaviorContext) {
        return "return confirm('" + confirmBean.getConfirmMsg() + "');";
    }
}
