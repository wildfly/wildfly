/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import org.apache.jasper.deploy.TagLibraryInfo;

import jakarta.servlet.descriptor.TaglibDescriptor;

/**
 * @author Stuart Douglas
 */
public class TaglibDescriptorImpl implements TaglibDescriptor {

    private final TagLibraryInfo tagLibraryInfo;

    public TaglibDescriptorImpl(TagLibraryInfo tagLibraryInfo) {
        this.tagLibraryInfo = tagLibraryInfo;
    }

    @Override
    public String getTaglibURI() {
        return tagLibraryInfo.getUri();
    }

    @Override
    public String getTaglibLocation() {
        return tagLibraryInfo.getLocation();
    }
}
