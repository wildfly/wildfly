/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.JDBCProtocolResourceDefinition.Attribute.DATA_SOURCE;

import javax.sql.DataSource;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.protocols.JDBC_PING;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class JDBCProtocolConfigurationServiceConfigurator extends ProtocolConfigurationServiceConfigurator<JDBC_PING> {

    private volatile SupplierDependency<DataSource> dataSource;

    public JDBCProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.dataSource.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.dataSource = new ServiceSupplierDependency<>(CommonUnaryRequirement.DATA_SOURCE.getServiceName(context, DATA_SOURCE.resolveModelAttribute(context, model).asString()));
        return super.configure(context, model);
    }

    @Override
    public void accept(JDBC_PING protocol) {
        protocol.setDataSource(this.dataSource.get());
    }
}
