/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.io.IOException;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;
import org.wildfly.plugin.tools.server.ServerManager;

/**
 * An abstract test used to update an attribute in the {@code jaxrs} subsystem.
 * <p>
 * Please note this intentionally does not use a {@link org.jboss.as.arquillian.api.ServerSetupTask}.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
abstract class AbstractResteasyAttributeTest {
    private static final ModelNode ADDRESS = Operations.createAddress("subsystem", "jaxrs");

    @ArquillianResource
    private static ServerManager serverManager;

    private final String attributeName;

    AbstractResteasyAttributeTest(final String attributeName) {
        this.attributeName = attributeName;
    }

    void writeAttribute(final ModelNode attributeValue) throws IOException {
        try {
            serverManager.executeOperation(Operations.createWriteAttributeOperation(ADDRESS, attributeName, attributeValue));
        } finally {
            serverManager.reloadIfRequired();
        }
    }

}
