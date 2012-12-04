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
package org.jboss.as.core.model.test.paths;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSpecifiedPathsTestCase extends AbstractCoreModelTest {

    private final TestModelType type;

    public AbstractSpecifiedPathsTestCase(TestModelType type) {
        this.type = type;
    }

    @Test
    public void testPaths() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
            .setXmlResource(getXmlResource())
            .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        String marshalled = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), getXmlResource()), marshalled);

        kernelServices = createKernelServicesBuilder()
                .setXml(marshalled)
                .build();
            Assert.assertTrue(kernelServices.isSuccessfulBoot());
    }

    @Test
    public void testEmptyPath() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
                .setModelInitializer(createEmptyModelInitalizer(), null)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        addOperationAddress(op, "ok");
        op.get(PATH).set("relative");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        op = new ModelNode();
        op.get(OP).set(ADD);
        addOperationAddress(op, "bad");
        kernelServices.executeForFailure(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(RELATIVE_TO).set("ok");
        addOperationAddress(op, "bad2");
        kernelServices.executeForFailure(op);

        ModelNode model = kernelServices.readWholeModel();
        model = getPathsParentModel(model);
        Assert.assertTrue(model.hasDefined(PATH));
        model = model.get(PATH);
        Assert.assertEquals(1, model.keys().size());
        model = model.get("ok");
        for (String key : model.keys()) {
            if (!model.hasDefined(key)) {
                model.remove(key);
            }
        }
        Assert.assertEquals("relative", model.get(PATH).asString());
    }

    void addOperationAddress(ModelNode op, String pathName) {
        for (PathElement element : getPathsParent()) {
            op.get(OP_ADDR).add(element.getKey(), element.getValue());
        }
        op.get(OP_ADDR).add(PATH, pathName);
    }

    ModelNode getPathsParentModel(ModelNode modelNode) {
        ModelNode result = modelNode;
        for (PathElement element : getPathsParent()) {
            result = modelNode.get(element.getKey(), element.getValue());
        }
        return result;
    }

    KernelServicesBuilder createKernelServicesBuilder() {
        return createKernelServicesBuilder(type);
    }

    protected abstract String getXmlResource();

    protected abstract PathAddress getPathsParent();

    protected ModelInitializer createEmptyModelInitalizer() {
        return new ModelInitializer() {
            @Override
            public void populateModel(Resource rootResource) {
                //Default is no-op
            }
        };
    }
}
