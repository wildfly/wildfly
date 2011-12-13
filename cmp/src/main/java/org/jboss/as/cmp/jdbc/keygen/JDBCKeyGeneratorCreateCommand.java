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

package org.jboss.as.cmp.jdbc.keygen;

import javax.ejb.CreateException;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCInsertPKCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.keygenerator.KeyGenerator;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;

/**
 * @author John Bailey
 */
public class JDBCKeyGeneratorCreateCommand extends JDBCInsertPKCreateCommand {
    protected KeyGenerator keyGenerator;
    protected JDBCCMPFieldBridge pkField;

    public void init(JDBCStoreManager manager) {
        super.init(manager);
        pkField = getGeneratedPKField();
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        super.initEntityCommand(entityCommand);

        String factoryName = entityCommand.getAttribute("key-generator-factory");
        if (factoryName == null) {
            throw new RuntimeException("key-generator-factory attribute must be set for entity " + entity.getEntityName());
        }
        try {
            KeyGeneratorFactory keyGeneratorFactory = entity.getManager().getKeyGeneratorFactory(factoryName);
            if(keyGeneratorFactory == null) {
                throw new IllegalArgumentException("Invalid key generator name; not found: " + factoryName);
            }
            keyGenerator = keyGeneratorFactory.getKeyGenerator();
        } catch (Exception e) {
            throw new RuntimeException("Error: can't create key generator instance; key generator factory: " + factoryName, e);
        }
    }

    protected void generateFields(CmpEntityBeanContext ctx) throws CreateException {
        super.generateFields(ctx);
        Object pk = keyGenerator.generateKey();
        log.debug("Generated new pk: " + pk);
        pkField.setInstanceValue(ctx, pk);
    }
}
