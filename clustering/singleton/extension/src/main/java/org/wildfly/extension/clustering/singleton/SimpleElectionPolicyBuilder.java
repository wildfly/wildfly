/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

/**
 * Builds a service that provides a simple election policy.
 * @author Paul Ferraro
 */
public class SimpleElectionPolicyBuilder extends ElectionPolicyBuilder {

    private volatile int position;

    public SimpleElectionPolicyBuilder(String name) {
        super(name);
    }

    @Override
    public SingletonElectionPolicy getValue() {
        return new SimpleSingletonElectionPolicy(this.position);
    }

    @Override
    public Builder<SingletonElectionPolicy> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.position = SimpleElectionPolicyResourceDefinition.Attribute.POSITION.getDefinition().resolveModelAttribute(context, model).asInt();
        return super.configure(context, model);
    }
}
