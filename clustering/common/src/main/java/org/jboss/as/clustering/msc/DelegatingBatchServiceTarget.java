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

package org.jboss.as.clustering.msc;

import java.util.Collection;

import org.jboss.msc.service.BatchServiceTarget;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public class DelegatingBatchServiceTarget extends DelegatingServiceTarget implements BatchServiceTarget {
    private final BatchServiceTarget target;

    public DelegatingBatchServiceTarget(BatchServiceTarget target, ServiceTargetFactory factory, BatchServiceTargetFactory batchFactory, ServiceBuilderFactory builderFactory) {
        super(target, factory, batchFactory, builderFactory);
        this.target = target;
    }

    @Override
    public BatchServiceTarget addListener(ServiceListener<Object> listener) {
        this.target.addListener(listener);
        return this;
    }

    @Override
    public BatchServiceTarget addListener(ServiceListener<Object>... listeners) {
        this.target.addListener(listeners);
        return this;
    }

    @Override
    public BatchServiceTarget addListener(Collection<ServiceListener<Object>> listeners) {
        this.target.addListener(listeners);
        return this;
    }

    @Override
    public BatchServiceTarget removeListener(ServiceListener<Object> listener) {
        this.target.removeListener(listener);
        return this;
    }

    @Override
    public BatchServiceTarget addDependency(ServiceName dependency) {
        this.target.addDependency(dependency);
        return this;
    }

    @Override
    public BatchServiceTarget addDependency(ServiceName... dependencies) {
        this.target.addDependency(dependencies);
        return this;
    }

    @Override
    public BatchServiceTarget addDependency(Collection<ServiceName> dependencies) {
        this.target.addDependency(dependencies);
        return this;
    }

    @Override
    public BatchServiceTarget removeDependency(ServiceName dependency) {
        this.target.removeDependency(dependency);
        return this;
    }

    @Override
    public void removeServices() {
        this.target.removeServices();
    }
}
