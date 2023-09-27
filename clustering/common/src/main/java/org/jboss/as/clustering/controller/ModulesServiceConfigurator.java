/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public class ModulesServiceConfigurator extends AbstractModulesServiceConfigurator<List<Module>> {

    private final List<Module> defaultModules;

    public ModulesServiceConfigurator(ServiceName name, Attribute attribute, List<Module> defaultModules) {
        super(name, attribute, ModelNode::asListOrEmpty);
        this.defaultModules = defaultModules;
    }

    @Override
    public List<Module> apply(List<Module> modules) {
        return modules.isEmpty() ? this.defaultModules : modules;
    }
}
