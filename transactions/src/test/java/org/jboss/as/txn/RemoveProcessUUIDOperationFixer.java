/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn;

import java.io.Serializable;

import org.jboss.as.model.test.OperationFixer;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemoveProcessUUIDOperationFixer implements OperationFixer, Serializable {
    private static final long serialVersionUID = 1L;
    static transient final RemoveProcessUUIDOperationFixer INSTANCE = new RemoveProcessUUIDOperationFixer();

    private RemoveProcessUUIDOperationFixer(){
    }

    @Override
    public ModelNode fixOperation(ModelNode operation) {
        if (operation.hasDefined("process-id-uuid") && operation.get("process-id-uuid").asString() == "false"){
            operation.remove("process-id-uuid");
        }
        return operation;
    }
}
