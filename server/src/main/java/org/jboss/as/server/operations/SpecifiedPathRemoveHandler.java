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
package org.jboss.as.server.operations;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.PathRemoveHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for removing a path.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SpecifiedPathRemoveHandler extends PathRemoveHandler implements RuntimeOperationHandler {

    public static SpecifiedPathRemoveHandler INSTANCE = new SpecifiedPathRemoveHandler();

    @Override
    protected void uninstallPath(String name, String path, String relativeTo, NewOperationContext context, ResultHandler resultHandler, ModelNode compensatingOp) {
        if (context instanceof NewRuntimeOperationContext) {
            NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> controller = runtimeContext.getServiceRegistry().getService(AbstractPathService.pathNameOf(name));
            if(controller == null) {
                resultHandler.handleResultComplete(compensatingOp);
            } else {
                controller.addListener(new ResultHandler.ServiceRemoveListener(resultHandler, compensatingOp));
            }
        }
        else {
            resultHandler.handleResultComplete(compensatingOp);
        }
    }
}
