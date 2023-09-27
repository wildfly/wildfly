/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.BeanState;
import org.jboss.msc.service.ServiceName;

import java.io.Serializable;

/**
 * The legacy depends meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DependsConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String dependency;
    private BeanState whenRequired = BeanState.INSTALLED;
    private BeanState dependencyState;
    private boolean service;

    @Override
    public void visit(ConfigVisitor visitor) {
        if (visitor.getState().equals(whenRequired)) {
            if (service)
                visitor.addDependency(ServiceName.parse(dependency));
            else
                visitor.addDependency(dependency, dependencyState);
        }
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }

    public void setWhenRequired(BeanState whenRequired) {
        this.whenRequired = whenRequired;
    }

    public void setDependencyState(BeanState dependencyState) {
        this.dependencyState = dependencyState;
    }

    public void setService(boolean service) {
        this.service = service;
    }
}