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

package org.jboss.as.cli.accesscontrol;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationRequestAddress;

/**
 * @author Alexey Loubyansky
 *
 */
public interface AccessRequirementBuilder {

    interface RequirementSetBuilder extends AccessRequirementBuilder {

        RequirementSetBuilder operation(String operation);

        RequirementSetBuilder operation(String address, String operation);

        RequirementSetBuilder serverGroupOperation(String operation);

        RequirementSetBuilder serverGroupOperation(String address, String operation);

        RequirementSetBuilder profileOperation(String address, String operation);

        RequirementSetBuilder operation(OperationRequestAddress address, String operation);

        RequirementSetBuilder serverGroupOperation(OperationRequestAddress address, String operation);

        RequirementSetBuilder profileOperation(OperationRequestAddress address, String operation);

        RequirementSetBuilder hostServerOperation(String address, String operation);

        RequirementSetBuilder requirement(AccessRequirement requirement);
    }

    RequirementSetBuilder all();

    RequirementSetBuilder any();

    AccessRequirementBuilder domain();

    AccessRequirementBuilder standalone();

    AccessRequirementBuilder parent();

    AccessRequirement build();

    class Factory {
        public static AccessRequirementBuilder create(final CommandContext ctx) {
            return new AccessRequirementBuilder() {

                AccessRequirementBuilder builder;

                @Override
                public RequirementSetBuilder all() {
                    if(builder != null) {
                        throw new IllegalStateException("The builder has been initialized: " + builder);
                    }
                    builder = new AllRequiredBuilder(this, ctx);
                    return (RequirementSetBuilder) builder;
                }

                @Override
                public RequirementSetBuilder any() {
                    if(builder != null) {
                        throw new IllegalStateException("The builder has been initialized: " + builder);
                    }
                    builder = new AnyRequiredBuilder(this, ctx);
                    return (RequirementSetBuilder) builder;
                }

                @Override
                public RequirementSetBuilder domain() {
                    if(builder != null) {
                        throw new IllegalStateException("The builder has been initialized: " + builder);
                    }
                    builder = new DomainModeRequirementBuilder(this, ctx);
                    return (RequirementSetBuilder) builder;
                }

                @Override
                public RequirementSetBuilder standalone() {
                    if(builder != null) {
                        throw new IllegalStateException("The builder has been initialized: " + builder);
                    }
                    builder = new StandaloneModeRequirementBuilder(this, ctx);
                    return (RequirementSetBuilder) builder;
                }

                @Override
                public AccessRequirementBuilder parent() {
                    throw new IllegalStateException();
                }

                @Override
                public AccessRequirement build() {
                    return builder == null ? AccessRequirement.NONE : builder.build();
                }
            };
        }

        private static class AllRequiredBuilder extends BaseRequirementSetBuilder {

            AllRequiredBuilder(AccessRequirementBuilder parent, CommandContext ctx) {
                super(parent, ctx);
            }

            @Override
            protected AccessRequirementSet createTarget() {
                return new AllRequiredSet();
            }
        }

        private static class AnyRequiredBuilder extends BaseRequirementSetBuilder {

            AnyRequiredBuilder(AccessRequirementBuilder parent, CommandContext ctx) {
                super(parent, ctx);
            }

            @Override
            protected AccessRequirementSet createTarget() {
                return new AnyRequiredSet();
            }
        }

        private abstract static class BaseRequirementSetBuilder implements RequirementSetBuilder {

            protected final AccessRequirementBuilder parent;
            protected final AccessRequirementSet set;
            protected final CommandContext ctx;

            BaseRequirementSetBuilder(AccessRequirementBuilder parent, CommandContext ctx) {
                this.parent = parent;
                set = createTarget();
                this.ctx = ctx;
                ctx.addEventListener(set);
            }

            protected abstract AccessRequirementSet createTarget();

            @Override
            public RequirementSetBuilder all() {
                final AllRequiredBuilder nested = new AllRequiredBuilder(this, ctx);
                set.add(nested.set);
                return nested;
            }

            @Override
            public RequirementSetBuilder any() {
                final AnyRequiredBuilder nested = new AnyRequiredBuilder(this, ctx);
                set.add(nested.set);
                return nested;
            }

            @Override
            public AccessRequirementBuilder domain() {
                final DomainModeRequirementBuilder nested = new DomainModeRequirementBuilder(this, ctx);
                set.add(nested.modeReq);
                return nested;
            }

            @Override
            public AccessRequirementBuilder standalone() {
                final StandaloneModeRequirementBuilder nested = new StandaloneModeRequirementBuilder(this, ctx);
                set.add(nested.modeReq);
                return nested;
            }

            @Override
            public RequirementSetBuilder operation(String operation) {
                return add(new MainOperationAccessRequirement(operation));
            }

            @Override
            public RequirementSetBuilder operation(OperationRequestAddress address, String operation) {
                return add(new MainOperationAccessRequirement(address, operation));
            }

            @Override
            public RequirementSetBuilder serverGroupOperation(String operation) {
                return add(new PerNodeOperationAccess(Util.SERVER_GROUP, operation));
            }

            @Override
            public RequirementSetBuilder serverGroupOperation(OperationRequestAddress address, String operation) {
                return add(new PerNodeOperationAccess(Util.SERVER_GROUP, address, operation));
            }

            @Override
            public RequirementSetBuilder profileOperation(OperationRequestAddress address, String operation) {
                return add(new PerNodeOperationAccess(Util.PROFILE, address, operation));
            }

            @Override
            public RequirementSetBuilder operation(String address, String operation) {
                return add(new MainOperationAccessRequirement(address, operation));
            }

            @Override
            public RequirementSetBuilder serverGroupOperation(String address, String operation) {
                return add(new PerNodeOperationAccess(Util.SERVER_GROUP, address, operation));
            }

            @Override
            public RequirementSetBuilder profileOperation(String address, String operation) {
                return add(new PerNodeOperationAccess(Util.PROFILE, address, operation));
            }

            @Override
            public RequirementSetBuilder hostServerOperation(String address, String operation) {
                return add(new HostServerOperationAccess(address, operation));
            }

            @Override
            public RequirementSetBuilder requirement(AccessRequirement requirement) {
                if(requirement == null) {
                    throw new IllegalArgumentException("requirement is null");
                }
                set.add(requirement);
                return this;
            }

            @Override
            public AccessRequirementBuilder parent() {
                return parent;
            }

            protected RequirementSetBuilder add(final BaseOperationAccessRequirement op) {
                set.add(op);
                ctx.addEventListener(op);
                return this;
            }

            @Override
            public AccessRequirement build() {
                return set;
            }
        }

        private static class DomainModeRequirementBuilder extends ControllerModeRequirementBuilder {

            DomainModeRequirementBuilder(AccessRequirementBuilder parent, CommandContext ctx) {
                super(parent, ctx);
            }

            @Override
            protected ControllerModeAccess createModeAccess() {
                return new ControllerModeAccess(ControllerModeAccess.Mode.DOMAIN);
            }
        }

        private static class StandaloneModeRequirementBuilder extends ControllerModeRequirementBuilder {

            StandaloneModeRequirementBuilder(AccessRequirementBuilder parent, CommandContext ctx) {
                super(parent, ctx);
            }

            @Override
            protected ControllerModeAccess createModeAccess() {
                return new ControllerModeAccess(ControllerModeAccess.Mode.STANDALONE);
            }
        }

        private abstract static class ControllerModeRequirementBuilder implements AccessRequirementBuilder {

            protected final AccessRequirementBuilder parent;
            protected final ControllerModeAccess modeReq;
            protected final CommandContext ctx;
            protected BaseRequirementSetBuilder nestedSet;

            ControllerModeRequirementBuilder(AccessRequirementBuilder parent, CommandContext ctx) {
                this.parent = parent;
                this.ctx = ctx;
                modeReq = createModeAccess();
                ctx.addEventListener(modeReq);
            }

            protected abstract ControllerModeAccess createModeAccess();

            @Override
            public RequirementSetBuilder all() {
                if(nestedSet != null) {
                    throw new IllegalStateException("The nested set has been initialized.");
                }
                nestedSet = new AllRequiredBuilder(this, ctx);
                modeReq.setRequirement(nestedSet.set);
                return nestedSet;
            }

            @Override
            public RequirementSetBuilder any() {
                if(nestedSet != null) {
                    throw new IllegalStateException("The nested set has been initialized.");
                }
                nestedSet = new AnyRequiredBuilder(this, ctx);
                modeReq.setRequirement(nestedSet.set);
                return nestedSet;
            }

            @Override
            public RequirementSetBuilder domain() {
                throw new IllegalStateException();
            }

            @Override
            public RequirementSetBuilder standalone() {
                throw new IllegalStateException();
            }

            @Override
            public AccessRequirementBuilder parent() {
                return parent;
            }

            @Override
            public AccessRequirement build() {
                return modeReq;
            }
        }
    }
}
