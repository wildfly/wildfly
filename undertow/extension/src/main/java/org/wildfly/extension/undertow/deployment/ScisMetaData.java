/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContainerInitializer;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * @author Remy Maucherat
 */
public class ScisMetaData {

    public static final AttachmentKey<ScisMetaData> ATTACHMENT_KEY = AttachmentKey.create(ScisMetaData.class);

    /**
     * SCIs.
     */
    private Set<ServletContainerInitializer> scis;

    /**
     * Handles types.
     */
    private Map<ServletContainerInitializer, Set<Class<?>>> handlesTypes;

    public Set<ServletContainerInitializer> getScis() {
        return scis;
    }

    public void setScis(Set<ServletContainerInitializer> scis) {
        this.scis = scis;
    }

    public Map<ServletContainerInitializer, Set<Class<?>>> getHandlesTypes() {
        return handlesTypes;
    }

    public void setHandlesTypes(Map<ServletContainerInitializer, Set<Class<?>>> handlesTypes) {
        this.handlesTypes = handlesTypes;
    }

}
