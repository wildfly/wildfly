/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import org.apache.jasper.deploy.JspPropertyGroup;

import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import java.util.Collection;

/**
 * @author Stuart Douglas
 */
public class JspPropertyGroupDescriptorImpl implements JspPropertyGroupDescriptor {

    private final JspPropertyGroup propertyGroup;

    public JspPropertyGroupDescriptorImpl(JspPropertyGroup propertyGroup) {
        this.propertyGroup = propertyGroup;
    }

    @Override
    public Collection<String> getUrlPatterns() {
        return propertyGroup.getUrlPatterns();
    }

    @Override
    public String getElIgnored() {
        return propertyGroup.getElIgnored();
    }

    @Override
    public String getPageEncoding() {
        return propertyGroup.getPageEncoding();
    }

    @Override
    public String getScriptingInvalid() {
        return propertyGroup.getScriptingInvalid();
    }

    @Override
    public String getIsXml() {
        return propertyGroup.getIsXml();
    }

    @Override
    public Collection<String> getIncludePreludes() {
        return propertyGroup.getIncludePreludes();
    }

    @Override
    public Collection<String> getIncludeCodas() {
        return propertyGroup.getIncludeCodas();
    }

    @Override
    public String getDeferredSyntaxAllowedAsLiteral() {
        return propertyGroup.getDeferredSyntaxAllowedAsLiteral();
    }

    @Override
    public String getTrimDirectiveWhitespaces() {
        return propertyGroup.getTrimDirectiveWhitespaces();
    }

    @Override
    public String getDefaultContentType() {
        return propertyGroup.getDefaultContentType();
    }

    @Override
    public String getBuffer() {
        return propertyGroup.getBuffer();
    }

    @Override
    public String getErrorOnUndeclaredNamespace() {
        return propertyGroup.getErrorOnUndeclaredNamespace();
    }

    @Override
    public String getErrorOnELNotFound() {
        return propertyGroup.getErrorOnELNotFound();
    }
}
