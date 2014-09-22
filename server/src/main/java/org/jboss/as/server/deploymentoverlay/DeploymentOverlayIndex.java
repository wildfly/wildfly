/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;

/**
 * Service that aggregates all available deployment overrides
 *
 * @author Stuart Douglas
 */
public class DeploymentOverlayIndex {

    private final Map<String, Map<String, byte[]>> exactMatches;
    private final Map<String, Map<String, byte[]>> wildcards;

    private DeploymentOverlayIndex(Map<String, Map<String, byte[]>> exactMatches, Map<String, Map<String, byte[]>> wildcards) {
        this.exactMatches = exactMatches;
        this.wildcards = wildcards;
    }

    public Map<String, byte[]> getOverlays(final String deployment) {
        Map<String, byte[]> ret = new HashMap<String, byte[]>();
        Map<String, byte[]> exact = exactMatches.get(deployment);
        if(exact != null) {
            ret.putAll(exact);
        }
        for(Map.Entry<String, Map<String, byte[]>> entry : wildcards.entrySet()) {
            if(getPattern(entry.getKey()).matcher(deployment).matches()) {
                for(Map.Entry<String, byte[]> e : entry.getValue().entrySet()) {
                    if(!ret.containsKey(e.getKey())) {
                        ret.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        return ret;
    }

    public static DeploymentOverlayIndex createDeploymentOverlayIndex(OperationContext context) {
        final Map<String, Map<String, byte[]>> exactMatches = new HashMap<String, Map<String, byte[]>>();
        final Map<String, Map<String, byte[]>> wildcards = new LinkedHashMap<String, Map<String, byte[]>>();
        Set<String> overlayNames = context.readResourceFromRoot(PathAddress.pathAddress(pathElement(DEPLOYMENT_OVERLAY))).getChildrenNames(DEPLOYMENT_OVERLAY);
        for (String overlay : overlayNames) {
            Set<String> deployments = context.readResourceFromRoot(PathAddress.pathAddress(pathElement(DEPLOYMENT_OVERLAY, overlay))).getChildrenNames(DEPLOYMENT);
            for (String deployment : deployments) {
                if (isWildcard(deployment)) {
                    handleContent(context, wildcards, overlay, deployment);
                } else {
                    handleContent(context, exactMatches, overlay, deployment);
                }
            }
        }
        //now we have the overlay names
        //lets get the content hashes
        return new DeploymentOverlayIndex(exactMatches, wildcards);
    }

    private static void handleContent(OperationContext context, Map<String, Map<String, byte[]>> wildcards, String overlay, String deployment) {
        Map<String, byte[]> contentMap = new HashMap<String, byte[]>();
        wildcards.put(deployment, contentMap);
        Set<String> content = context.readResourceFromRoot(PathAddress.pathAddress(pathElement(DEPLOYMENT_OVERLAY, overlay))).getChildrenNames(CONTENT);
        for(String contentItem : content) {
            Resource cr = context.readResourceFromRoot(PathAddress.pathAddress(pathElement(DEPLOYMENT_OVERLAY, overlay), pathElement(CONTENT, contentItem)));
            ModelNode sha;
            try {
                sha = DeploymentOverlayContentDefinition.CONTENT.resolveModelAttribute(context, cr.getModel());
            } catch (OperationFailedException e) {
                throw new RuntimeException(e);
            }
            String key = contentItem.startsWith("/") ? contentItem.substring(1) : contentItem;
            contentMap.put(key, sha.asBytes());
        }
    }

    private static boolean isWildcard(String name) {
        return name.contains("*") || name.contains("?");
    }

    private static Pattern getPattern(String name) {
        return Pattern.compile(wildcardToJavaRegexp(name));
    }

    private static String wildcardToJavaRegexp(String expr) {
        if(expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        String regex = expr.replaceAll("([(){}\\[\\].+^$])", "\\\\$1"); // escape regex characters
        regex = regex.replaceAll("\\*", ".*"); // replace * with .*
        regex = regex.replaceAll("\\?", "."); // replace ? with .
        return regex;
    }
}
