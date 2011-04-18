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
package org.jboss.as.cli.operation;

import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public interface OperationRequestBuilder {

    /**
     * Sets the name operation to be invoked.
     *
     * @param name the name of the operation to invoke.
     */
    void setOperationName(String name);

    /**
     * The address is specified as a path to the target node. Each element of the path is a node
     * and is identified by its type and name.
     *
     * @param type the type of the node
     * @param name the name of the node
     */
    void addNode(String type, String name);

    /**
     * This method is supposed to be invoked from applying the prefix with ends on a node type.
     * @param type  the type of the node.
     */
    void addNodeType(String type);

    /**
     * This method assumes there is a non-empty prefix which ends on a node type.
     * Otherwise, this method will result in an exception.
     * @param name the name of the node for the type specified by the prefix.
     */
    void addNodeName(String name);

    /**
     * Adds an argument.
     *
     * @param name the name of the argument
     * @param value the value of the argument
     */
    void addProperty(String name, String value);

    /**
     * Builds the operation request based on the collected operation name, address and arguments.
     *
     * @return an instance of ModelNode representing the operation request
     * @throws OperationFormatException
     */
    ModelNode buildRequest() throws OperationFormatException;
}
