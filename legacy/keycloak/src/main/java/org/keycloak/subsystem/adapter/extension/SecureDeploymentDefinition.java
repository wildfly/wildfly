/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.keycloak.subsystem.adapter.extension;

/**
 * Defines attributes and operations for a secure-deployment.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
final class SecureDeploymentDefinition extends AbstractAdapterConfigurationDefinition {

    static final String TAG_NAME = "secure-deployment";

    public SecureDeploymentDefinition() {
        super(TAG_NAME, ALL_ATTRIBUTES_ARRAY);
    }

}
