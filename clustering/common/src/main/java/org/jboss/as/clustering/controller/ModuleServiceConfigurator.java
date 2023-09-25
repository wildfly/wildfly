/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collections;
import java.util.List;

import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

/**
 * Configures a service providing a {@link Module}.
 * @author Paul Ferraro
 */
public class ModuleServiceConfigurator extends AbstractModulesServiceConfigurator<Module> {

    public ModuleServiceConfigurator(ServiceName name, Attribute attribute) {
        super(name, attribute, Collections::singletonList);
    }

    @Override
    public Module apply(List<Module> modules) {
        return !modules.isEmpty() ? modules.get(0) : null;
    }
}
