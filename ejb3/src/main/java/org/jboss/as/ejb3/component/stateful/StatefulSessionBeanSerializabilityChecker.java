/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.marshalling.SerializabilityChecker;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
public class StatefulSessionBeanSerializabilityChecker implements SerializabilityChecker {

    private final Value<ModuleDeployment> deployment;

    public StatefulSessionBeanSerializabilityChecker(Value<ModuleDeployment> deployment) {
        this.deployment = deployment;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.SerializabilityChecker#isSerializable(java.lang.Class)
     */
    @Override
    public boolean isSerializable(Class<?> targetClass) {
        if (targetClass == Object.class) return false;
        if (DEFAULT.isSerializable(targetClass)) return true;
        for (EjbDeploymentInformation info: this.deployment.getValue().getEjbs().values()) {
            EJBComponent component = info.getEjbComponent();
            if ((component instanceof StatefulSessionComponent) && targetClass.isAssignableFrom(component.getComponentClass())) {
                return true;
            }
        }
        return false;
    }
}
