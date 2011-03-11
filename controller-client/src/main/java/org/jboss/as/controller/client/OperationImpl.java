/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;

class OperationImpl implements OperationBuilder, Operation {

    private final ModelNode operation;
    private volatile List<InputStream> inputStreams;

    OperationImpl(ModelNode operation) {
        this.operation = operation;
    }

    @Override
    public ModelNode getOperation() {
        return operation;
    }

    @Override
    public List<InputStream> getInputStreams() {
        if (inputStreams == null) {
            return Collections.emptyList();
        }
        return inputStreams;
    }

    @Override
    public OperationBuilder addInputStream(InputStream in) {
        if (inputStreams == null) {
            inputStreams = new ArrayList<InputStream>();
        }
        inputStreams.add(in);
        return this;
    }

    @Override
    public int getInputStreamCount() {
        List<InputStream> list = inputStreams;
        return list == null ? 0 : list.size();
    }

    @Override
    public Operation build() {
        return this;
    }


    @Override
    public Operation clone() {
        OperationImpl copy = new OperationImpl(operation.clone());
        if (inputStreams != null) {
            copy.inputStreams = new ArrayList<InputStream>(inputStreams);
        }
        return copy;
    }

    @Override
    public Operation clone(final ModelNode operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Null operation");
        }
        OperationImpl copy = new OperationImpl(operation);
        if (inputStreams != null) {
            copy.inputStreams = new ArrayList<InputStream>(inputStreams);
        }
        return copy;
    }
}