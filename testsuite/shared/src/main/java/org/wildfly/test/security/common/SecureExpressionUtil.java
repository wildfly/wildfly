/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
