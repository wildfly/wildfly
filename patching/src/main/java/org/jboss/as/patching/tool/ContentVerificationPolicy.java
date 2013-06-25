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

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;

/**
 * Policy for content verification.
 *
 * @author Emanuel Muckenhuber
 */
public interface ContentVerificationPolicy {

    /**
     * Update existing Policy.
     *
     * Ignored content validation.
     *
     * @param item the content item
     * @return whether a mismatch leads to an abort in the patch execution
     */
    boolean ignoreContentValidation(final ContentItem item);

    /**
     * Preserve existing Policy.
     *
     * Excluded content execution.
     *
     * @param item the content item
     * @return whether the execution of the content task should be skipped
     */
    boolean preserveExisting(final ContentItem item);

    ContentVerificationPolicy STRICT = new ContentVerificationPolicy() {

        @Override
        public boolean ignoreContentValidation(ContentItem item) {
            return false;
        }

        @Override
        public boolean preserveExisting(ContentItem item) {
            return false;
        }
    };

    ContentVerificationPolicy OVERRIDE_ALL = new ContentVerificationPolicy() {

        @Override
        public boolean ignoreContentValidation(ContentItem item) {
            return true;
        }

        @Override
        public boolean preserveExisting(ContentItem item) {
            return false;
        }
    };

    ContentVerificationPolicy PRESERVE_ALL = new ContentVerificationPolicy() {

        @Override
        public boolean ignoreContentValidation(ContentItem item) {
            return false;
        }

        @Override
        public boolean preserveExisting(ContentItem item) {
            return item.getContentType() == ContentType.MISC;
        }
    };

}
