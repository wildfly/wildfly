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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.dmr.ModelNode;

/**
 * {@link ProfileParsingCompletionHandler} that installs a default JSF extension and subsystem if the
 * profile included legacy web subsystem versions and did not include a JSF subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DefaultJsfProfileCompletionHandler implements ProfileParsingCompletionHandler {

    private static final String JSF_NAMESPACE_PREFIX = "urn:jboss:domain:jsf:";

    private static final String JSF_SUBSYSTEM = "jsf";

    private static final String JSF_MODULE = "org.jboss.as.jsf";

    @Override
    public void handleProfileParsingCompletion(Map<String, List<ModelNode>> profileBootOperations, List<ModelNode> otherBootOperations) {

        List<ModelNode> legacyWebOps = null;

        // Check all namespace versions less than 1.4 (the first in a release where the JSF subsystem was added)
        for (Namespace namespace : EnumSet.allOf(Namespace.class)) {
            String namespaceName = namespace.getUriString();
            if (namespaceName != null && namespaceName.startsWith("urn:jboss:domain:web:1.")) {
                String sub = namespaceName.substring("urn:jboss:domain:web:1.".length());
                if (sub.length() > 0
                        && ('0' == sub.charAt(0) || '1' == sub.charAt(0) || '2' == sub.charAt(0) || '3' == sub.charAt(0))
                        && (sub.length() == 1 || '.' == sub.charAt(1))) {
                    legacyWebOps = profileBootOperations.get(namespace.getUriString());
                    if (legacyWebOps != null) {
                        break;
                    }
                }
            }
        }

        if (legacyWebOps != null) {
            boolean hasJsf = false;
            for (String namespace : profileBootOperations.keySet()) {
                if (namespace.startsWith(JSF_NAMESPACE_PREFIX)) {
                    hasJsf = true;
                    break;
                }
            }

            if (!hasJsf) {

                // See if we need to add the extension as well
                boolean hasJSFExtension = false;
                for (ModelNode op : otherBootOperations) {
                    PathAddress pa = PathAddress.pathAddress(op.get(OP_ADDR));
                    if (pa.size() == 1 && EXTENSION.equals(pa.getElement(0).getKey())
                            && JSF_MODULE.equals(pa.getElement(0).getValue())) {
                        hasJSFExtension = true;
                        break;
                    }
                }

                if (!hasJSFExtension) {
                    final ModelNode jsfExtension = new ModelNode();
                    jsfExtension.get(OP).set(ADD);
                    PathAddress jsfExtensionAddress = PathAddress.pathAddress(PathElement.pathElement(EXTENSION, JSF_MODULE));
                    jsfExtension.get(OP_ADDR).set(jsfExtensionAddress.toModelNode());
                    jsfExtension.get(MODULE).set(JSF_MODULE);
                    otherBootOperations.add(jsfExtension);
                }

                final ModelNode jsf = new ModelNode();
                jsf.get(OP).set(ADD);
                PathAddress jsfAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JSF_SUBSYSTEM));
                jsf.get(OP_ADDR).set(jsfAddress.toModelNode());
                legacyWebOps.add(jsf);
            }
        }
    }
}
