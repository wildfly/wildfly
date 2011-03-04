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


/**
 *
 * @author Alexey Loubyansky
 */
public interface OperationRequestParser {

    interface CallbackHandler {

        void start(String operationString);

        void rootNode();

        void parentNode();

        void nodeType();

        void nodeType(String nodeType) throws OperationFormatException;

        void nodeTypeNameSeparator(int index);

        void nodeName(String nodeName) throws OperationFormatException;

        void nodeSeparator(int index);

        void addressOperationSeparator(int index);

        void operationName(String operationName) throws OperationFormatException;

        void propertyListStart(int index);

        void propertyName(String propertyName) throws OperationFormatException;

        void propertyNameValueSeparator(int index);

        void property(String name, String value, int nameValueSeparatorIndex) throws OperationFormatException;

        void propertySeparator(int index);

        void propertyListEnd(int index);

        // TODO this is not good
        void nodeTypeOrName(String typeOrName) throws OperationFormatException;
    }

    void parse(String operationRequest, CallbackHandler handler) throws OperationFormatException;
}
