/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Ensures the default domain.xml and host.xml start.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class DefaultConfigSmokeTestCase extends BuildConfigurationTestBase {
    private static final Logger LOGGER = Logger.getLogger(DefaultConfigSmokeTestCase.class);

    public static final String secondaryAddress = System.getProperty("jboss.test.host.secondary.address", "127.0.0.1");

    @Test
    public void testStandardHost() throws Exception {
        final WildFlyManagedConfiguration config = createConfiguration("domain.xml", "host.xml", getClass().getSimpleName());
        final DomainLifecycleUtil utils = new DomainLifecycleUtil(config);
        try {
            utils.start();
            // Double-check server status by confirming server-one can accept a web request to the root
            URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(PRIMARY_ADDRESS) + ":8080").openConnection();
            connection.connect();

            if (Boolean.getBoolean("expression.audit")) {
                writeExpressionAudit(utils);
            }
        } finally {
            utils.stop(); // Stop
        }
    }

    @Test
    public void testPrimaryAndSecondary() throws Exception {
        final WildFlyManagedConfiguration primaryConfig = createConfiguration("domain.xml", "host-primary.xml", getClass().getSimpleName());
        final DomainLifecycleUtil primaryUtils = new DomainLifecycleUtil(primaryConfig);
        final WildFlyManagedConfiguration secondaryConfig = createConfiguration("domain.xml", "host-secondary.xml", getClass().getSimpleName(),
                "secondary", secondaryAddress, 19990);
        final DomainLifecycleUtil secondaryUtils = new DomainLifecycleUtil(secondaryConfig);
        try {
            primaryUtils.start();
            secondaryUtils.start();
            // Double-check server status by confirming server-one can accept a web request to the root
            URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(secondaryAddress) + ":8080").openConnection();
            connection.connect();
        } finally {
            try {
                secondaryUtils.stop();
            } finally {
                primaryUtils.stop();
            }
        }
    }

    private void writeExpressionAudit(final DomainLifecycleUtil utils) throws IOException {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(RECURSIVE).set(true);

        final ModelNode result = utils.getDomainClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.hasDefined(RESULT));

        PathAddress pa = PathAddress.EMPTY_ADDRESS;
        writeExpressionAudit(pa, result.get(RESULT));
    }

    private static void writeExpressionAudit(PathAddress pa, ModelNode resourceDescription) {
        String paString = getPaString(pa);
        if (LOGGER.isTraceEnabled()) {
            if (resourceDescription.hasDefined(ModelDescriptionConstants.ATTRIBUTES)) {
                for (Property property : resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).asPropertyList()) {
                    ModelNode attrdesc = property.getValue();
                    if (!attrdesc.hasDefined(ModelDescriptionConstants.STORAGE) ||
                            AttributeAccess.Storage.CONFIGURATION.name().toLowerCase(Locale.ENGLISH).equals(attrdesc.get(ModelDescriptionConstants.STORAGE).asString().toLowerCase(Locale.ENGLISH))) {
                        StringBuilder sb = new StringBuilder(paString);
                        sb.append(",").append(property.getName());
                        sb.append(",").append(attrdesc.get(ModelDescriptionConstants.TYPE).asString());
                        sb.append(",").append(attrdesc.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).asBoolean(false));
                        sb.append(",").append(attrdesc.get(ModelDescriptionConstants.DESCRIPTION).asString());
                        LOGGER.trace(sb.toString());
                    }
                }
            }
        }

        if (resourceDescription.hasDefined(ModelDescriptionConstants.CHILDREN)) {
            for (Property childTypeProp : resourceDescription.get(ModelDescriptionConstants.CHILDREN).asPropertyList()) {
                String childType = childTypeProp.getName();
                ModelNode childTypeDesc = childTypeProp.getValue();
                if (childTypeDesc.hasDefined(ModelDescriptionConstants.MODEL_DESCRIPTION)) {
                    for (Property childInstanceProp : childTypeDesc.get(ModelDescriptionConstants.MODEL_DESCRIPTION).asPropertyList()) {
                        PathAddress childAddress = pa.append(childType, childInstanceProp.getName());
                        writeExpressionAudit(childAddress, childInstanceProp.getValue());
                    }
                }
            }
        }

    }

    private static String getPaString(PathAddress pa) {
        if (pa.size() == 0) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (PathElement pe : pa) {
            sb.append("/").append(pe.getKey()).append("=").append(pe.getValue());
        }
        return sb.toString();
    }
}
