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

package org.jboss.as.cmp.subsystem;

import java.util.Locale;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.as.cmp.keygenerator.hilo.HiLoKeyGeneratorFactory;
import static org.jboss.as.cmp.subsystem.CmpConstants.BLOCK_SIZE;
import static org.jboss.as.cmp.subsystem.CmpConstants.CREATE_TABLE;
import static org.jboss.as.cmp.subsystem.CmpConstants.CREATE_TABLE_DDL;
import static org.jboss.as.cmp.subsystem.CmpConstants.DATA_SOURCE;
import static org.jboss.as.cmp.subsystem.CmpConstants.DROP_TABLE;
import static org.jboss.as.cmp.subsystem.CmpConstants.ID_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpConstants.SELECT_HI_DDL;
import static org.jboss.as.cmp.subsystem.CmpConstants.SEQUENCE_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpConstants.SEQUENCE_NAME;
import static org.jboss.as.cmp.subsystem.CmpConstants.TABLE_NAME;
import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class HiLoKeyGeneratorAdd extends AbstractKeyGeneratorAdd {
    static HiLoKeyGeneratorAdd INSTANCE = new HiLoKeyGeneratorAdd();

    private HiLoKeyGeneratorAdd() {
    }

    protected Service<KeyGeneratorFactory> getKeyGeneratorFactory(final ModelNode operation) {
        final HiLoKeyGeneratorFactory factory = new HiLoKeyGeneratorFactory();
        if (operation.hasDefined(BLOCK_SIZE)) {
            factory.setBlockSize(operation.get(BLOCK_SIZE).asLong());
        }
        if (operation.hasDefined(CREATE_TABLE)) {
            factory.setCreateTable(operation.get(CREATE_TABLE).asBoolean());
        }
        if (operation.hasDefined(CREATE_TABLE_DDL)) {
            factory.setCreateTableDdl(operation.get(CREATE_TABLE_DDL).asString());
        }
        if (operation.hasDefined(DROP_TABLE)) {
            factory.setDropTable(operation.get(DROP_TABLE).asBoolean());
        }
        if (operation.hasDefined(ID_COLUMN)) {
            factory.setIdColumnName(operation.get(ID_COLUMN).asString());
        }
        if (operation.hasDefined(SELECT_HI_DDL)) {
            factory.setSelectHiSql(operation.get(SELECT_HI_DDL).asString());
        }
        if (operation.hasDefined(SEQUENCE_COLUMN)) {
            factory.setSequenceColumn(operation.get(SEQUENCE_COLUMN).asString());
        }
        if (operation.hasDefined(SEQUENCE_NAME)) {
            factory.setSequenceName(operation.get(SEQUENCE_NAME).asString());
        }
        if (operation.hasDefined(TABLE_NAME)) {
            factory.setTableName(operation.get(TABLE_NAME).asString());
        }
        return factory;
    }

    protected ServiceName getServiceName(String name) {
        return HiLoKeyGeneratorFactory.SERVICE_NAME.append(name);
    }

    protected void addDependencies(final ModelNode operation, final Service<KeyGeneratorFactory> keyGeneratorFactory, final ServiceBuilder<KeyGeneratorFactory> factoryServiceBuilder) {
        final HiLoKeyGeneratorFactory hiloFactory = HiLoKeyGeneratorFactory.class.cast(keyGeneratorFactory);
        factoryServiceBuilder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(operation.get(DATA_SOURCE).asString()), DataSource.class, hiloFactory.getDataSourceInjector());
        factoryServiceBuilder.addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, hiloFactory.getTransactionManagerInjector());
    }

    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        setOnModel(operation, model, BLOCK_SIZE);
        setOnModel(operation, model, CREATE_TABLE);
        setOnModel(operation, model, CREATE_TABLE_DDL);
        setOnModel(operation, model, DROP_TABLE);
        setOnModel(operation, model, ID_COLUMN);
        setOnModel(operation, model, SELECT_HI_DDL);
        setOnModel(operation, model, SEQUENCE_COLUMN);
        setOnModel(operation, model, SEQUENCE_NAME);
        setOnModel(operation, model, TABLE_NAME);
    }

    private void setOnModel(final ModelNode operation, final ModelNode model, final String attName) {
        if (operation.hasDefined(attName)) {
            model.get(attName).set(operation.get(attName));
        }
    }

    public ModelNode getModelDescription(final Locale locale) {
        return CmpSubsystemDescriptions.getHiLoAddDescription(locale);
    }
}
