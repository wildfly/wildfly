/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.wildfly.common.Assert;
import org.wildfly.security.permission.AbstractNameSetOnlyPermission;
import org.wildfly.security.util.StringEnumeration;
import org.wildfly.security.util.StringMapping;

/**
 * A general batch permission.  The permission {@code name} must be one of the following:
 * <ul>
 * <li>{@code start}</li>
 * <li>{@code stop}</li>
 * <li>{@code restart}</li>
 * <li>{@code abandon}</li>
 * <li>{@code read}</li>
 * </ul>
 * The {@code actions} are not used and should be empty or {@code null}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class BatchPermission extends AbstractNameSetOnlyPermission<BatchPermission> {

    private static final long serialVersionUID = 6124294238228442419L;

    private static final StringEnumeration strings = StringEnumeration.of(
            "start",
            "stop",
            "restart",
            "abandon",
            "read"
    );

    private static final StringMapping<BatchPermission> mapping = new StringMapping<>(strings, BatchPermission::new);

    private static final BatchPermission allPermission = new BatchPermission("*");

    /**
     * Construct a new instance.
     *
     * @param name the name of the permission
     */
    public BatchPermission(final String name) {
        this(name, null);
    }

    /**
     * Construct a new instance.
     *
     * @param name    the name of the permission
     * @param actions the actions (should be empty)
     */
    public BatchPermission(final String name, final String actions) {
        super(name, strings);
        requireEmptyActions(actions);
    }

    public BatchPermission withName(final String name) {
        return forName(name);
    }

    /**
     * Get the permission with the given name.
     *
     * @param name the name (must not be {@code null})
     *
     * @return the permission (not {@code null})
     *
     * @throws IllegalArgumentException if the name is not valid
     */
    public static BatchPermission forName(final String name) {
        Assert.checkNotNullParam("name", name);
        return "*".equals(name) ? allPermission : mapping.getItemByString(name);
    }
}