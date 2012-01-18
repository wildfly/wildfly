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
package org.jboss.as.cli.operation.impl;

import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.ParsedOperationRequestHeader;

/**
 *
 * @author Alexey Loubyansky
 */
public class RolloutPlanCompleter implements CommandLineCompleter {

    public static final RolloutPlanCompleter INSTANCE = new RolloutPlanCompleter();

    private static final DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
    static {
        address.toNode(Util.MANAGEMENT_CLIENT_CONTENT, Util.ROLLOUT_PLANS);
    }

    private final DefaultCallbackHandler parsedOp = new DefaultCallbackHandler();

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext, java.lang.String, int, java.util.List)
     */
    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
        if(!ctx.isDomainMode()) {
            return -1;
        }
        if(buffer.isEmpty()) {
            candidates.add("{rollout");
            return 0;
        }

        try {
            parsedOp.parseOperation(null, buffer);
        } catch (CommandFormatException e) {
            return -1;
        }
        if(parsedOp.isRequestComplete()) {
            return -1;
        }

        if(parsedOp.endsOnHeaderListStart() || parsedOp.endsOnHeaderSeparator()) {
            candidates.add("rollout");
            return parsedOp.getLastSeparatorIndex() + 1;
        }
        final List<ParsedOperationRequestHeader> headers = parsedOp.getHeaders();
        if(headers.isEmpty()) {
            if(ctx.getParsedCommandLine().getOriginalLine().endsWith(" ") /* '{rollout ' */) {
                final String originalLine = ctx.getParsedCommandLine().getOriginalLine();
                int bufferIndex = originalLine.lastIndexOf(buffer);
                if(bufferIndex == -1) { // that's illegal state
                    return -1;
                }
                candidates.add("id=");
                candidates.addAll(Util.getServerGroups(ctx.getModelControllerClient()));
                return originalLine.length() - bufferIndex;
            }
            return buffer.length();
        }
        final ParsedOperationRequestHeader lastHeader = headers.get(headers.size() - 1);
        if(!(lastHeader instanceof ParsedRolloutPlanHeader)) {
            throw new IllegalStateException("Expected " + ParsedRolloutPlanHeader.class + " but got " + lastHeader);
        }
        final ParsedRolloutPlanHeader rollout = (ParsedRolloutPlanHeader) lastHeader;

        if(rollout.endsOnPlanIdValueSeparator()) {
            candidates.addAll(Util.getNodeNames(ctx.getModelControllerClient(), address, Util.ROLLOUT_PLAN));
            return rollout.getLastSeparatorIndex() + 1;
        }

        final String planRef = rollout.getPlanRef();
        if(planRef != null) {
            final List<String> nodeNames = Util.getNodeNames(ctx.getModelControllerClient(), address, Util.ROLLOUT_PLAN);
            for(String name : nodeNames) {
                if(name.startsWith(planRef)) {
                    candidates.add(name);
                }
            }
            return rollout.getLastChunkIndex();
        }

        if(rollout.hasProperties()) {
            final String lastName = rollout.getLastPropertyName();
            if(Util.ROLLBACK_ACROSS_GROUPS.startsWith(lastName)) {
                candidates.add(Util.ROLLBACK_ACROSS_GROUPS + '}');
            }
            return rollout.getLastChunkIndex();
        }

        if(rollout.endsOnGroupSeparator()) {
            final List<String> serverGroups = Util.getServerGroups(ctx.getModelControllerClient());
            for(String group : serverGroups) {
                if(!rollout.containsGroup(group)) {
                    candidates.add(group);
                }
            }
            return buffer.length();
        }

        final SingleRolloutPlanGroup lastGroup = rollout.getLastGroup();
        if(lastGroup == null) {
            return -1;
        }

        if(lastGroup.endsOnPropertyListEnd()) {
            candidates.add("^");
            candidates.add(",");
            if(Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
                // header properties
                candidates.add(Util.ROLLBACK_ACROSS_GROUPS);
            }
            candidates.add("}");
            return buffer.length();
        }

        if(lastGroup.endsOnPropertyListStart()) {
            candidates.add(Util.MAX_FAILED_SERVERS + '=');
            candidates.add(Util.MAX_FAILURE_PERCENTAGE + '=');
            candidates.add(Util.ROLLING_TO_SERVERS + '=');
            return buffer.length();
        }

        if(lastGroup.hasProperties()) {
            final String propValue = lastGroup.getLastPropertyValue();
            if(propValue != null) {
                if(Util.TRUE.startsWith(propValue)) {
                    candidates.add(Util.TRUE);
                } else if(Util.FALSE.startsWith(propValue)) {
                    candidates.add(Util.FALSE);
                } else {
                    return buffer.length();
                }
            } else if(lastGroup.endsOnPropertyValueSeparator()) {
                if(Util.ROLLING_TO_SERVERS.equals(lastGroup.getLastPropertyName())) {
                    candidates.add(Util.FALSE);
                    candidates.add(Util.TRUE);
                }
                return buffer.length();
            } else if(lastGroup.endsOnPropertySeparator()) {
                if(!lastGroup.hasProperty(Util.MAX_FAILED_SERVERS)) {
                    candidates.add(Util.MAX_FAILED_SERVERS + '=');
                }
                if(!lastGroup.hasProperty(Util.MAX_FAILURE_PERCENTAGE)) {
                    candidates.add(Util.MAX_FAILURE_PERCENTAGE + '=');
                }
                if(!lastGroup.hasProperty(Util.ROLLING_TO_SERVERS)) {
                    candidates.add(Util.ROLLING_TO_SERVERS + '=');
                }
                return lastGroup.getLastSeparatorIndex() + 1;
            } else {
                final String propName = lastGroup.getLastPropertyName();
                if(Util.MAX_FAILED_SERVERS.startsWith(propName)) {
                    candidates.add(Util.MAX_FAILED_SERVERS + '=');
                }
                if(Util.MAX_FAILURE_PERCENTAGE.startsWith(propName)) {
                    candidates.add(Util.MAX_FAILURE_PERCENTAGE + '=');
                } else if(Util.ROLLING_TO_SERVERS.startsWith(propName)) {
                    candidates.add(Util.ROLLING_TO_SERVERS + '=');
                }
            }
            return lastGroup.getLastChunkIndex();
        }

        if(Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
            candidates.add("(");
            candidates.add("^");
            candidates.add(",");
            // header propersties
            candidates.add(Util.ROLLBACK_ACROSS_GROUPS);
            candidates.add("}");
            return buffer.length();
        }

        int result = lastGroup.getLastChunkIndex();

        final String groupName = lastGroup.getGroupName();
        final List<String> serverGroups = Util.getServerGroups(ctx.getModelControllerClient());
        for(String group : serverGroups) {
            if(group.startsWith(groupName)) {
                candidates.add(group);
            }
        }
        if(Util.ID.startsWith(groupName)) {
            candidates.add("id=");
        } else {
            if (candidates.size() == 1) {
                final String group = candidates.get(0);
                candidates.set(0, group + '(');
                candidates.add(group + ',');
                candidates.add(group + '^');
                candidates.add(group + '}');
            }
        }

        return result;
    }

}
