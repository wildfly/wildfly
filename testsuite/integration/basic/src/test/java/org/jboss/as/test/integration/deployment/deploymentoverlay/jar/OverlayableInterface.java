/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import jakarta.ejb.Remote;

/**
 * @author baranowb
 *
 */
@Remote
public interface OverlayableInterface {
    String ORIGINAL = "ORIGINAL";
    String OVERLAYED = "OVERLAYED";
    String RESOURCE_NAME = "file.txt";
    String RESOURCE_META_INF = "x/"+RESOURCE_NAME;
    String RESOURCE = "META-INF/"+RESOURCE_META_INF;

    String STATIC = "STATIC";
    String RESOURCE_STATIC_NAME = "static.txt";
    String RESOURCE_STATIC_META_INF = "x/"+RESOURCE_STATIC_NAME;
    String RESOURCE_STATIC = "META-INF/"+RESOURCE_STATIC_META_INF;

    String fetchResource() throws Exception;

    String fetchResourceStatic() throws Exception;
}
