/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.logging;

import java.io.IOException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.junit.Assert.*;

import org.jboss.as.controller.client.helpers.Operations;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class LogFilterTestCase extends AbstractOperationsTestCase {

    private KernelServices kernelServices;

    @Before
    public void bootKernelServices() throws Exception {
        kernelServices = boot();
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/default-subsystem.xml");
    }

    @Test
    public void addLoggingFilter() throws IOException {
        ModelNode consoleAddress = createAddress("console-handler", "CONSOLE").toModelNode();
        ModelNode replaceValue = new ModelNode();
        replaceValue.get("pattern").set("JBAS");
        replaceValue.get("replacement").set("DUMMY");
        replaceValue.get("replace-all").set(true);
        ModelNode filterAttributeValue = new ModelNode();
        filterAttributeValue.get("replace").set(replaceValue);

        final ModelNode writeOp = Operations.createWriteAttributeOperation(consoleAddress, "filter", filterAttributeValue);
        ModelNode result = executeOperation(kernelServices, writeOp);
        // Create the read operation
        final ModelNode readAttributeOp = Operations.createReadAttributeOperation(consoleAddress, "filter");
        result = executeOperation(kernelServices, readAttributeOp);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(OUTCOME).asString(), is("success"));
        assertEquals("{\"replace\" => {\"replace-all\" => true,\"pattern\" => \"JBAS\",\"replacement\" => \"DUMMY\"}}",
                Operations.readResult(result).asString());
        ModelNode readResourceOp = Operations.createReadResourceOperation(consoleAddress);
        result = executeOperation(kernelServices, readResourceOp);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(OUTCOME).asString(), is("success"));
        assertThat(result.get(RESULT).hasDefined("filter-spec"), is(true));
        ModelNode filterSpec = result.get(RESULT).get("filter-spec");
        assertThat(filterSpec.asString(), is("substituteAll(\"JBAS\",\"DUMMY\")"));

        assertThat(result.get(RESULT).hasDefined("filter"), is(true));
        assertThat(result.get(RESULT).get("filter").hasDefined("replace"), is(true));
        ModelNode replaceResult = result.get(RESULT).get("filter").get("replace");
        assertThat(replaceResult.hasDefined("pattern"), is(true));
        assertThat(replaceResult.get("pattern").asString(), is("JBAS"));
        assertThat(replaceResult.hasDefined("replacement"), is(true));
        assertThat(replaceResult.get("replacement").asString(), is("DUMMY"));
        assertThat(replaceResult.hasDefined("pattern"), is(true));
        assertThat(replaceResult.get("pattern").asString(), is("JBAS"));
    }
}
