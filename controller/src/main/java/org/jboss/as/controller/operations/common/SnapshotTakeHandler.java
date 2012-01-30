/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.SnapshotDescriptions;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.dmr.ModelNode;

/**
 * An operation that takes a snapshot of the current configuration
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SnapshotTakeHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "take-snapshot";

    private final ConfigurationPersister persister;
    /** Slave DC should not take snapshot */
    private final boolean takeSnapshot;

    public SnapshotTakeHandler(ConfigurationPersister persister) {
        this(persister, true);
    }

    public SnapshotTakeHandler(ConfigurationPersister persiter, boolean takeSnapshot) {
        this.persister = persiter;
        this.takeSnapshot = takeSnapshot;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        try {
            if (takeSnapshot) {
                String name = persister.snapshot();
                context.getResult().get().set(name);
            }

            context.completeStep();
        } catch (ConfigurationPersistenceException e) {
            throw new OperationFailedException(e.getMessage(), new ModelNode().set(e.getMessage()));
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SnapshotDescriptions.getSnapshotTakeModel(locale);
    }
}
