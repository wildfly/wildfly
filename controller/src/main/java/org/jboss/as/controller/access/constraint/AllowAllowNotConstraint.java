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

package org.jboss.as.controller.access.constraint;

import org.jboss.as.controller.access.Action;

/**
 * A constraint meant for use in situations where the Action or Target "is" or "is not" something
 * and the user "allows" or "allowsNot".
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class AllowAllowNotConstraint extends AbstractConstraint {

    private final Boolean is;
    private final Boolean allows;
    private final Boolean allowsNot;

    protected AllowAllowNotConstraint(boolean is) {
        super();
        this.is = is;
        this.allows = this.allowsNot = null;
    }

    protected AllowAllowNotConstraint(boolean allows, boolean allowsNot) {
        super();
        this.is = null;
        this.allows = allows;
        this.allowsNot = allowsNot;
    }

    @Override
    public boolean violates(Constraint other, Action.ActionEffect actionEffect) {
        if (other.getClass() == getClass()) {
            AllowAllowNotConstraint aanc = (AllowAllowNotConstraint) other;
            if (is == null) {
                assert aanc.is != null : "incompatible comparison of user and required constraints";
                return aanc.is ? !allows : !allowsNot;
            } else {
                assert aanc.is == null : "incompatible comparison of user and required constraints";
                return is ? !aanc.allows : !aanc.allowsNot;
            }
        }
        return false;
    }
}
