/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * User: jpai
 */
public interface EEResourceReferenceProcessor {

    String getResourceReferenceType();

    InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException;
}
