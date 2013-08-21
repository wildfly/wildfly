/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test.util;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TransformersTestParameters {

    private final ModelVersion modelVersion;
    private final ModelTestControllerVersion testControllerVersion;

    public TransformersTestParameters(ModelVersion modelVersion, ModelTestControllerVersion testControllerVersion) {
        this.modelVersion = modelVersion;
        this.testControllerVersion = testControllerVersion;
    }

    protected TransformersTestParameters(TransformersTestParameters delegate) {
        this(delegate.getModelVersion(), delegate.getTestControllerVersion());
    }

    public ModelVersion getModelVersion() {
        return modelVersion;
    }

    public ModelTestControllerVersion getTestControllerVersion() {
        return testControllerVersion;
    }

    public static List<Object[]> setupVersions(){
        List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] {new TransformersTestParameters(ModelVersion.create(1, 2, 0), ModelTestControllerVersion.V7_1_2_FINAL)});
        data.add(new Object[] {new TransformersTestParameters(ModelVersion.create(1, 3, 0), ModelTestControllerVersion.V7_1_3_FINAL)});
        data.add(new Object[] {new TransformersTestParameters(ModelVersion.create(1, 4, 0), ModelTestControllerVersion.MASTER)});
        for (int i = 0 ; i < data.size() ; i++) {
            Object[] entry = data.get(i);
            System.out.println("Parameter " + i + ": " + entry[0]);
        }
        return data;
    }

    @Override
    public String toString() {
        return "TransformersTestParameters={modelVersion=" + modelVersion + "; testControllerVersion=" + testControllerVersion + "}";
    }


}
