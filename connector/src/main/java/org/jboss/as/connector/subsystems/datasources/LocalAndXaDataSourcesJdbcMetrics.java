/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import java.util.Set;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.statistics.JdbcStatisticsPlugin;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class LocalAndXaDataSourcesJdbcMetrics implements OperationStepHandler {

    static LocalAndXaDataSourcesJdbcMetrics INSTANCE = new LocalAndXaDataSourcesJdbcMetrics();

    static final Set<String> ATTRIBUTES = (new JdbcStatisticsPlugin()).getNames();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String jndiName = address.getLastElement().getValue();
                    final String attributeName = operation.require(NAME).asString();

                    final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                            ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
                    if (managementRepoService != null) {
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            final ModelNode result = context.getResult();
                            if (repository.getDataSources() != null) {
                                for (DataSource ds : repository.getDataSources()) {
                                    if (jndiName.equalsIgnoreCase(ds.getJndiName())) {
                                        if (ds.getStatistics() != null) {
                                            result.set("" + ds.getStatistics().getValue(attributeName));
                                        }

                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set("failed to get metrics " + e.getMessage()));
                        }
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep();
    }
}
