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
package org.jboss.as.core.model.test.systemproperty;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyReferencesTestCase extends AbstractCoreModelTest {

    @Before
    public void clearProperties() {
        System.clearProperty("test.one");
        System.clearProperty("test.two");
        System.clearProperty("test.referencing");


    }

    @Test
    public void testSystemPropertyReferences() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE).build();
        Assert.assertNull(System.getProperty("test.one"));
        Assert.assertNull(System.getProperty("test.two"));
        Assert.assertNull(System.getProperty("test.referencing"));

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("system-property", "test.one");
        op.get(VALUE).set("ONE");
        kernelServices.executeForResult(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("system-property", "test.two");
        op.get(VALUE).set("TWO");
        kernelServices.executeForResult(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("system-property", "test.referencing");
        op.get(VALUE).setExpression("${test.one} ${test.two}");
        kernelServices.executeForResult(op);

        Assert.assertEquals("ONE", System.getProperty("test.one"));
        Assert.assertEquals("TWO", System.getProperty("test.two"));
        Assert.assertEquals("ONE TWO", System.getProperty("test.referencing"));

        op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add("system-property", "test.referencing");
        op.get(NAME).set(VALUE);
        op.get(VALUE).setExpression("${test.one}---${test.two}");
        kernelServices.executeForResult(op);

        Assert.assertEquals("ONE---TWO", System.getProperty("test.referencing"));
    }
}