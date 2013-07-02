/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.tool;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;

/**
 * @author Emanuel Muckenhuber
 */
class ContentPolicyBuilderImpl implements PatchTool.ContentPolicyBuilder {

    boolean overrideAll;
    boolean ignoreModulesChanges;
    final List<String> override = new ArrayList<String>();
    final List<String> preserve = new ArrayList<String>();

    @Override
    public ContentVerificationPolicy createPolicy() {
        return new ContentVerificationPolicy() {
            @Override
            public boolean ignoreContentValidation(ContentItem item) {
                final ContentType type = item.getContentType();
                if(type == ContentType.MODULE || type == ContentType.BUNDLE) {
                    return ignoreModulesChanges | overrideAll;
                }
                final MiscContentItem misc = (MiscContentItem) item;
                final String path = misc.getRelativePath();
                if(override.contains(path)) {
                   return true;
                }
                // Preserve should skip content verification
                if(preserve.contains(path)) {
                    return true;
                }
                return overrideAll;
            }

            @Override
            public boolean preserveExisting(ContentItem item) {
                final ContentType type = item.getContentType();
                if(type == ContentType.MISC) {
                    final MiscContentItem misc = (MiscContentItem) item;
                    final String path = misc.getRelativePath();
                    return preserve.contains(path);
                }
                return false;
            }
        };
    }

    @Override
    public PatchTool.ContentPolicyBuilder ignoreModuleChanges() {
        ignoreModulesChanges = true;
        return this;
    }

    @Override
    public PatchTool.ContentPolicyBuilder overrideItem(MiscContentItem item) {
        return overrideItem(item.getRelativePath());
    }

    @Override
    public PatchTool.ContentPolicyBuilder overrideItem(String path) {
        override.add(path);
        return this;
    }

    @Override
    public PatchTool.ContentPolicyBuilder preserveItem(MiscContentItem item) {
        return preserveItem(item.getRelativePath());
    }

    @Override
    public PatchTool.ContentPolicyBuilder preserveItem(String path) {
        preserve.add(path);
        return this;
    }

    @Override
    public PatchTool.ContentPolicyBuilder overrideAll() {
        overrideAll = true;
        return this;
    }

}
