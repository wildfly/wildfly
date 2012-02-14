/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;

/**
 * Default static resource configs.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DefaultJspConfig {

    private static final Map<String, ModelNode> defaults = new HashMap<String, ModelNode>();

    static {

        defaults.put(Constants.DEVELOPMENT, new ModelNode().set(false));
        defaults.put(Constants.DISABLED, new ModelNode().set(false));
        defaults.put(Constants.KEEP_GENERATED, new ModelNode().set(true));
        defaults.put(Constants.TRIM_SPACES, new ModelNode().set(false));
        defaults.put(Constants.TAG_POOLING, new ModelNode().set(true));
        defaults.put(Constants.MAPPED_FILE, new ModelNode().set(true));
        defaults.put(Constants.CHECK_INTERVAL, new ModelNode().set(0));
        defaults.put(Constants.MODIFICATION_TEST_INTERVAL, new ModelNode().set(4));
        defaults.put(Constants.RECOMPILE_ON_FAIL, new ModelNode().set(false));
        defaults.put(Constants.SMAP, new ModelNode().set(true));
        defaults.put(Constants.DUMP_SMAP, new ModelNode().set(false));
        defaults.put(Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, new ModelNode().set(false));
        defaults.put(Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, new ModelNode().set(false));
        defaults.put(Constants.SOURCE_VM, new ModelNode().set("1.5"));
        defaults.put(Constants.TARGET_VM, new ModelNode().set("1.5"));
        defaults.put(Constants.JAVA_ENCODING, new ModelNode().set("UTF8"));
        defaults.put(Constants.DISPLAY_SOURCE_FRAGMENT, new ModelNode().set(true));
    }

    static ModelNode getDefaultStaticResource() {
        ModelNode result = new ModelNode();
        for (Map.Entry<String, ModelNode> entry : defaults.entrySet()) {
            result.get(entry.getKey()).set(entry.getValue());
        }
        return result;
    }

    static boolean hasNotDefault(ModelNode model, String key) {
        return model.hasDefined(key) && !model.get(key).equals(defaults.get(key));
    }

    static ModelNode getDefaultIfUndefined(String key, ModelNode toCheck) {
        ModelNode result = toCheck;
        if (!toCheck.isDefined()) {
            ModelNode ours = defaults.get(key);
            if (ours != null) {
                result = ours;
            }
        }
        return result;
    }
}
