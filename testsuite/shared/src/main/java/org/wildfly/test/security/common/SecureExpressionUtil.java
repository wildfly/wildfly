/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

public final class SecureExpressionUtil {

    public static final class SecureExpressionData extends org.jboss.as.test.shared.SecureExpressionUtil.SecureExpressionData {
        private final String property;

        public SecureExpressionData(String clearText, String property) {
            super(clearText);
            this.property = property;
        }
    }

    public static void setupCredentialStoreExpressions(String storeName,
                                                       SecureExpressionData... toConfigure) throws Exception {
        org.jboss.as.test.shared.SecureExpressionUtil.setupCredentialStoreExpressions(storeName, toConfigure);
    }

    public static void setupCredentialStore(ManagementClient arquillianClient, String storeName, String storeLocation) throws Exception {
        org.wildfly.core.testrunner.ManagementClient client = getCoreManagmentClient(arquillianClient);
        org.jboss.as.test.shared.SecureExpressionUtil.setupCredentialStore(client, storeName, storeLocation);
    }

    public static void teardownCredentialStore(ManagementClient arquillianClient, String storeName, String storeLocation) throws Exception {

        org.wildfly.core.testrunner.ManagementClient client = getCoreManagmentClient(arquillianClient);
        org.jboss.as.test.shared.SecureExpressionUtil.teardownCredentialStore(client, storeName, storeLocation);
    }

    public static Asset getDeploymentPropertiesAsset(SecureExpressionData... expressions) {
        StringBuilder builder = new StringBuilder("# Conversion of well known static properties to dynamic secure " +
                "expressions calculated by " + SecureExpressionUtil.class.getSimpleName() + " during test setup\n");
        if (expressions != null) {
            for (SecureExpressionData expressionData : expressions) {
                if (expressionData.property != null && !expressionData.property.isEmpty()) {
                    builder.append(expressionData.property);
                    builder.append('=');
                    builder.append(expressionData.getExpression());
                    builder.append('\n');
                }
            }
        }
        return new StringAsset(builder.toString());
    }

    public static Class[] getDeploymentClasses() {
        return new Class[] { SecureExpressionUtil.class,
                SecureExpressionData.class,
                org.jboss.as.test.shared.SecureExpressionUtil.class,
                org.jboss.as.test.shared.SecureExpressionUtil.SecureExpressionData.class
        };
    }

    private static org.wildfly.core.testrunner.ManagementClient getCoreManagmentClient(ManagementClient arquillianClient) {
        return new org.wildfly.core.testrunner.ManagementClient(arquillianClient.getControllerClient(), arquillianClient.getMgmtAddress(), arquillianClient.getMgmtPort(), arquillianClient.getMgmtProtocol());
    }
}
