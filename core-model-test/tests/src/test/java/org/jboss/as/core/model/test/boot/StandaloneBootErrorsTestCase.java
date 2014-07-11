/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.core.model.test.boot;

import java.util.List;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_ERROR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_ERRORS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class StandaloneBootErrorsTestCase extends AbstractBootErrorTestCase {

    public StandaloneBootErrorsTestCase() {
        super(TestModelType.STANDALONE);
    }

    @Override
    protected String getXmlResource() {
        return "standalone.xml";
    }

    @Test
    public void testBootErrors() throws Exception {
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
        ModelNode readBootErrorsOp = Util.createOperation("read-boot-errors", PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT)));
        ModelNode result = kernelServices.executeForResult(readBootErrorsOp);
        Assert.assertThat(result, is(notNullValue()));
        Assert.assertThat(result.hasDefined(BOOT_ERRORS), is(true));
        List<ModelNode> errors = result.get(BOOT_ERRORS).asList();
        Assert.assertThat(errors.size(), is(3));
        ModelNode error = errors.get(0).get(BOOT_ERROR);
        Assert.assertThat(error.get(FAILED).get(OP).asString(), is(ADD));
        Assert.assertThat(error.get(FAILED).get(ADDRESS).asString(), is("[(\"core-service\" => \"management\"),(\"access\" => \"audit\"),(\"syslog-handler\" => \"syslog-tls\")]"));
        Assert.assertThat(error.hasDefined(FAILURES), is(true));
        Assert.assertThat(error.get(FAILURES).asString(), is("[\"testhost\"]"));
        error = errors.get(1).get(BOOT_ERROR);
        Assert.assertThat(error.get(FAILED).get(OP).asString(), is(ADD));
        Assert.assertThat(error.get(FAILED).get(ADDRESS).asString(), is("[(\"core-service\" => \"management\"),(\"access\" => \"audit\"),(\"syslog-handler\" => \"syslog-tcp\")]"));
        Assert.assertThat(error.hasDefined(FAILURES), is(true));
        Assert.assertThat(error.get(FAILURES).asString(), is("[\"testhost\"]"));
        error = errors.get(2).get(BOOT_ERROR);
        Assert.assertThat(error.get(FAILED).get(OP).asString(), is(ADD));
        Assert.assertThat(error.get(FAILED).get(ADDRESS).asString(), is("[(\"core-service\" => \"management\"),(\"access\" => \"audit\"),(\"syslog-handler\" => \"syslog-udp\")]"));
        Assert.assertThat(error.hasDefined(FAILURES), is(true));
        Assert.assertThat(error.get(FAILURES).asString(), is("[\"testhost\"]"));
    }
}
