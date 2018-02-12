/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.JDBCProtocolResourceDefinition.Attribute.*;

import javax.sql.DataSource;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.jgroups.protocol.JDBCProtocol;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class JDBCProtocolConfigurationBuilder<P extends Protocol & JDBCProtocol> extends ProtocolConfigurationBuilder<P> {

    private volatile ValueDependency<DataSource> dataSource;

    public JDBCProtocolConfigurationBuilder(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceBuilder<ProtocolConfiguration<P>> build(ServiceTarget target) {
        return this.dataSource.register(super.build(target));
    }

    @Override
    public Builder<ProtocolConfiguration<P>> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.dataSource = new InjectedValueDependency<>(CommonUnaryRequirement.DATA_SOURCE.getServiceName(context, DATA_SOURCE.resolveModelAttribute(context, model).asString()), DataSource.class);
        return super.configure(context, model);
    }

    @Override
    public void accept(P protocol) {
        protocol.setDataSource(this.dataSource.getValue());
    }
}
