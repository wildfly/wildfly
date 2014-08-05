/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.msc;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

/**
 * Encapsulates the common logic for building an injected value service.
 * @author Paul Ferraro
 */
public class InjectedValueServiceBuilder {

    private final ServiceTarget target;

    public InjectedValueServiceBuilder(ServiceTarget target) {
        this.target = target;
    }

    public <T> ServiceBuilder<T> build(ServiceName name, ServiceName targetName, Class<T> targetClass) {
        InjectedValue<T> value = new InjectedValue<>();
        return this.target.addService(name, new ValueService<>(value))
                .addDependency(targetName, targetClass, value)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }
}
