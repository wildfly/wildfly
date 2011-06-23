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
package org.jboss.as.host.controller.operations;


import java.util.Locale;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import org.jboss.as.host.controller.descriptions.HostServerDescription;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} removing an existing server configuration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static final ServerRemoveHandler INSTANCE = new ServerRemoveHandler();

    /**
     * Create the InterfaceRemoveHandler
     */
    protected ServerRemoveHandler() {
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostServerDescription.getServerRemoveOperation(locale);
    }
}
