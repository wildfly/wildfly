/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.subsystem;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.jpa.hibernate.HibernatePersistenceProviderAdaptor;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderAdapterRegistry;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.processor.JPAAnnotationParseProcessor;
import org.jboss.as.jpa.processor.JPADependencyProcessor;
import org.jboss.as.jpa.processor.PersistenceRefProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitDeploymentProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitParseProcessor;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * Add the JPA subsystem directive.
 * <p/>
 * TODO:  add subsystem configuration properties
 *
 * @author Scott Marlow
 */

class JPASubSystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final JPASubSystemAdd INSTANCE = new JPASubSystemAdd();

    private JPASubSystemAdd() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        if (context instanceof BootOperationContext) {

            /* set Hibernate persistence provider as the default provider */
            javax.persistence.spi.PersistenceProviderResolverHolder.setPersistenceProviderResolver(
                PersistenceProviderResolverImpl.getInstance());

            PersistenceProviderAdapterRegistry.putPersistenceProviderAdaptor(
                "org.hibernate.ejb.HibernatePersistence", new HibernatePersistenceProviderAdaptor());

            final BootOperationContext updateContext = (BootOperationContext) context;
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_PERSISTENCE_UNIT, new PersistenceUnitParseProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_PERSISTENCE_ANNOTATION, new JPAAnnotationParseProcessor());
            updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JPA, new JPADependencyProcessor());
            // TODO: enable updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_PERSISTENCE_PROVIDER, new PersistenceProviderProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_PERSISTENCE_REF, new PersistenceRefProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_PERSISTENTUNIT, new PersistenceUnitDeploymentProcessor());
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        context.getSubModel().setEmptyObject();
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }

}
