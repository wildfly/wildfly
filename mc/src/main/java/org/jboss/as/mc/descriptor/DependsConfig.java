/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;

import java.io.Serializable;

/**
 * The legacy depends meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DependsConfig implements Serializable, ConfigVisitorNode {
    private static final long serialVersionUID = 1L;

    private String dependency;
    private BeanState whenRequired = BeanState.INSTALLED;
    private BeanState dependencyState;

    @Override
    public void visit(ConfigVisitor visitor) {
        if (visitor.getState().equals(whenRequired)) {
            visitor.addDependency(dependency, dependencyState);
        }
    }

    public String getDependency() {
        return dependency;
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }

    public BeanState getWhenRequired() {
        return whenRequired;
    }

    public void setWhenRequired(BeanState whenRequired) {
        this.whenRequired = whenRequired;
    }

    public BeanState getDependencyState() {
        return dependencyState;
    }

    public void setDependencyState(BeanState dependencyState) {
        this.dependencyState = dependencyState;
    }
}