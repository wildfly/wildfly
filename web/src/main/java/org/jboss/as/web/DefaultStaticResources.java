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
public class DefaultStaticResources {

    private static final Map<String, ModelNode> defaults = new HashMap<String, ModelNode>();

    static {
        defaults.put(Constants.LISTINGS, new ModelNode().set(false));
        defaults.put(Constants.SENDFILE, new ModelNode().set(49152));
        defaults.put(Constants.READ_ONLY, new ModelNode().set(true));
        defaults.put(Constants.WEBDAV, new ModelNode().set(false));
        defaults.put(Constants.MAX_DEPTH, new ModelNode().set(3));
        defaults.put(Constants.DISABLED, new ModelNode().set(false));
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
