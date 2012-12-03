/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AbstractCoreModelTest {

    private final CoreModelTestDelegate delegate;

    protected AbstractCoreModelTest() {
        delegate = new CoreModelTestDelegate(this.getClass());
    }

    @Before
    public void initializeParser() throws Exception {
        delegate.initializeParser();
    }

    @After
    public void cleanup() throws Exception {
        delegate.cleanup();
    }

    protected KernelServicesBuilder createKernelServicesBuilder(TestModelType type) {
        return delegate.createKernelServicesBuilder(type);
    }

    /**
     * Checks that the transformed model is the same as the model built up in the legacy subsystem controller via the transformed operations,
     * and that the transformed model is valid according to the resource definition in the legacy subsystem controller.
     *
     * @param kernelServices the main kernel services
     * @param modelVersion   the model version of the targetted legacy subsystem
     * @return the whole model of the legacy controller
     */
    protected ModelNode checkCoreModelTransformation(KernelServices kernelServices, ModelVersion modelVersion) throws IOException {
        return checkCoreModelTransformation(kernelServices, modelVersion, null, null);
    }

    /**
     * Checks that the transformed model is the same as the model built up in the legacy subsystem controller via the transformed operations,
     * and that the transformed model is valid according to the resource definition in the legacy subsystem controller.
     *
     * @param kernelServices the main kernel services
     * @param modelVersion   the model version of the targetted legacy subsystem
     * @param legacyModelFixer use to touch up the model read from the legacy controller, use sparingly when the legacy model is just wrong. May be {@code null}
     * @return the whole model of the legacy controller
     */
    protected ModelNode checkCoreModelTransformation(KernelServices kernelServices, ModelVersion modelVersion, ModelFixer legacyModelFixer, ModelFixer transformedModelFixer) throws IOException {
        return delegate.checkCoreModelTransformation(kernelServices, modelVersion, legacyModelFixer, transformedModelFixer);
    }
}
