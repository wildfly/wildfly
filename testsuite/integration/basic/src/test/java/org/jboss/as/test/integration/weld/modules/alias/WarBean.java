/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.alias;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
 */
@ApplicationScoped
public class WarBean {

    @Inject
    private ModuleBean moduleBean;

    public ModuleBean getInjectedBean() {
        return moduleBean;
    }
}
