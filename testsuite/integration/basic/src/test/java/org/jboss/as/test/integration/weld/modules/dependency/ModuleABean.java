/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.dependency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@ApplicationScoped
public class ModuleABean {

    @Inject
    private ModuleBBean moduleBBean;

    public String ping() {
        return ModuleABean.class.getSimpleName();
    }

    public String pingModuleBBean() {
        return moduleBBean.ping();
    }
}
