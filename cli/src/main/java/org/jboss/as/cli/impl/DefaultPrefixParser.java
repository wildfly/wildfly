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
package org.jboss.as.cli.impl;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Prefix;
import org.jboss.as.cli.PrefixParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultPrefixParser implements PrefixParser {

    private static final char NODE_SEPARATOR = ',';
    private static final char NODE_TYPE_NAME_SEPARATOR = '=';
    private static final String ROOT = "~";
    private static final String PARENT_NODE = "..";
    private static final String NODE_TYPE = ".type";

    /* (non-Javadoc)
     * @see org.jboss.as.cli.PrefixParser#parse(java.lang.String, org.jboss.as.cli.Prefix)
     */
    @Override
    public void parse(String str, Prefix prefix) throws CommandFormatException {

        if(ROOT.equals(str)) {
            prefix.reset();
            return;
        }

        if(str.startsWith(ROOT)) {
            prefix.reset();
            str = str.substring(1).trim();
        }

        int nodeIndex = 0;
        while (nodeIndex < str.length()) {
            int nodeSepIndex = str.indexOf(NODE_SEPARATOR, nodeIndex);
            if (nodeSepIndex < 0) {
                nodeSepIndex = str.length();
            }
            String node = str.substring(nodeIndex, nodeSepIndex).trim();
            if (node.isEmpty()) {
                throw new CommandFormatException(
                        "Node type/name is missing or the format is wrong for the prefix '"
                                + str + "'");
            }

            String nodeType = null;
            String nodeName = null;
            int nameValueSep = node.indexOf(NODE_TYPE_NAME_SEPARATOR);
            if (nameValueSep < 0) {
                if(PARENT_NODE.equals(node)) {
                    prefix.toParentNode();
                } else if(NODE_TYPE.equals(node)) {
                    prefix.toNodeType();
                } else if(!prefix.endsOnType()) {
                    // node type only and it must be the last one in the prefix
                    if(nodeSepIndex != str.length())
                        throw new CommandFormatException("Node name is missing after node type '" + node +
                            "' or the format of the prefix is wrong.");
                    nodeType = node;
                } else {
                    nodeName = node;
                }
            } else {
                nodeType = node.substring(0, nameValueSep).trim();
                nodeName = node.substring(nameValueSep + 1).trim();
            }

            if (nodeType != null && !Util.isValidIdentifier(nodeType)) {
                throw new CommandFormatException(
                        "The node type is not a valid identifier '"
                                + nodeType
                                + "' or the format is wrong for prefix '"
                                + str + "'");
            }

            if (nodeName != null && !Util.isValidIdentifier(nodeName)) {
                throw new CommandFormatException(
                        "The node name is not a valid identifier '"
                                + nodeName
                                + "' or the format is wrong for prefix '"
                                + str + "'");
            }

            if(nodeType == null) {
                if(nodeName != null)
                   prefix.toNode(nodeName);
            } else if(!prefix.endsOnType()) {
                prefix.toNode(nodeType, nodeName);
            } else {
                throw new CommandFormatException("Can't go to node '" + node +
                        "' since the current prefix ends on type, specify the node name first.");
            }

            nodeIndex = nodeSepIndex + 1;
        }
    }

}
