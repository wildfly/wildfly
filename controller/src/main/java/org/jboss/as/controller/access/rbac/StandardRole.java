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

package org.jboss.as.controller.access.rbac;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.access.Action;

/**
 * The standard roles in the WildFly management access control mechanism.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public enum StandardRole {

    MONITOR("Monitor", Action.ActionEffect.READ_CONFIG, Action.ActionEffect.READ_RUNTIME),
    //CONFIGURATOR,
    OPERATOR("Operator", Action.ActionEffect.READ_CONFIG, Action.ActionEffect.READ_RUNTIME, Action.ActionEffect.WRITE_RUNTIME),
    MAINTAINER("Maintainer"),
    DEPLOYER("Deployer"),
    ADMINISTRATOR("Administrator"),
    AUDITOR("Auditor"),
    SUPERUSER("SuperUser");

    private final String name;
    private final Set<Action.ActionEffect> allowedActions;

    private StandardRole(String name) {
        this(name, Action.ActionEffect.values());
    }

    private StandardRole(String name, Action.ActionEffect... allowedExcludingAccess) {
        this(name, EnumSet.of(Action.ActionEffect.ADDRESS, allowedExcludingAccess));
    }

    private StandardRole(String name, Set<Action.ActionEffect> allowedActions) {
        this.name = name;
        this.allowedActions = allowedActions;
    }

    public boolean isActionEffectAllowed(Action.ActionEffect actionEffect) {
        return allowedActions.contains(actionEffect);
    }

    public String getFormalName() {
        return name;
    }

    public String getOfficialForm() {
        return toString().toUpperCase(Locale.ENGLISH);
    }

}
