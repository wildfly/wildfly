/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.deployment;

import org.apache.jasper.deploy.JspPropertyGroup;

import javax.servlet.descriptor.JspPropertyGroupDescriptor;
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
}
