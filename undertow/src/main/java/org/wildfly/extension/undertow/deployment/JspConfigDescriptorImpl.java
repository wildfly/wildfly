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
import org.apache.jasper.deploy.TagLibraryInfo;

import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Stuart Douglas
 */
public class JspConfigDescriptorImpl implements JspConfigDescriptor {

    private final Collection<TaglibDescriptor> taglibs;
    private final Collection<JspPropertyGroupDescriptor> jspPropertyGroups;

    public JspConfigDescriptorImpl(Collection<TagLibraryInfo> taglibs, Collection<JspPropertyGroup> jspPropertyGroups) {
        this.taglibs = new ArrayList<TaglibDescriptor>();
        for(TagLibraryInfo t : taglibs) {
            this.taglibs.add(new TaglibDescriptorImpl(t));
        }
        this.jspPropertyGroups = new ArrayList<JspPropertyGroupDescriptor>();
        for(JspPropertyGroup p : jspPropertyGroups) {
            this.jspPropertyGroups.add(new JspPropertyGroupDescriptorImpl(p));
        }
    }

    @Override
    public Collection<TaglibDescriptor> getTaglibs() {
        return new ArrayList<TaglibDescriptor>(taglibs);
    }

    @Override
    public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
        return new ArrayList<JspPropertyGroupDescriptor>(jspPropertyGroups);
    }
}
