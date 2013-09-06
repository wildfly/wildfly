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

package org.jboss.as.cli.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.accesscontrol.AccessRequirement;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class PermittedCandidates implements DefaultCompleter.CandidatesProvider {

    public static class ValueWithAccessRequirement {

        private final String value;
        private final AccessRequirement requirement;

        public ValueWithAccessRequirement(String value, AccessRequirement requirement) {
            if(value == null) {
                throw new IllegalArgumentException("Value is null");
            }
            if(requirement == null) {
                throw new IllegalArgumentException("access requirement is null");
            }
            this.value = value;
            this.requirement = requirement;
        }

        void visit(CommandContext ctx, List<String> allowed) {
            if(requirement.isSatisfied(ctx)) {
                allowed.add(value);
            }
        }
    }

    private static class StaticPermittedCandidates extends PermittedCandidates {

        private final List<ValueWithAccessRequirement> values = new ArrayList<ValueWithAccessRequirement>();

        @Override
        protected List<ValueWithAccessRequirement> getValues(CommandContext ctx) {
            return values;
        }

        @Override
        protected void add(ValueWithAccessRequirement value) {
            values.add(value);
        }
    }

    public static PermittedCandidates create(String value, AccessRequirement requirement) {
        final PermittedCandidates provider = new StaticPermittedCandidates();
        return provider.add(value, requirement);
    }

    protected abstract List<ValueWithAccessRequirement> getValues(CommandContext ctx);

    protected abstract void add(ValueWithAccessRequirement value);

    public PermittedCandidates add(String value, AccessRequirement requirement) {
        add(new ValueWithAccessRequirement(value, requirement));
        return this;
    }

    @Override
    public Collection<String> getAllCandidates(CommandContext ctx) {
        final List<String> allowed = new ArrayList<String>();
        if(ctx.getConfig().isAccessControl()) {
            for(ValueWithAccessRequirement value : getValues(ctx)) {
                value.visit(ctx, allowed);
            }
        } else {
            for(ValueWithAccessRequirement value : getValues(ctx)) {
                allowed.add(value.value);
            }
        }
        return allowed;
    }
}
