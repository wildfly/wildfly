/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.naming.java.permission;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Iterator;

import org.jboss.as.naming.logging.NamingLogger;
import org.wildfly.common.Assert;

/**
 * Permission to access an object within the "java:" JNDI namespace.
 * <p>
 * This permission does not span into bound nested contexts; such contexts may be governed by their own permission scheme.
 * <p>
 * The {@code name} segment of the permission is a JNDI path whose segments are separated by {@code /} characters.  The
 * name may be preceded with the string {@code java:} for compatibility with previous schemes.  A name of
 * {@code &lt;&lt;ALL BINDINGS&gt;&gt;} is translated to {@code -} for compatibility reasons.
 */
public final class JndiPermission extends Permission {
    private static final long serialVersionUID = 1272655825146515997L;

    private final int actionBits;
    private String actionString;

    /**
     * The bitwise encoding of the {@code bind} action.
     */
    public static final int ACTION_BIND                 = 0b000000001;
    /**
     * The bitwise encoding of the {@code rebind} action.
     */
    public static final int ACTION_REBIND               = 0b000000010;
    /**
     * The bitwise encoding of the {@code unbind} action.
     */
    public static final int ACTION_UNBIND               = 0b000000100;
    /**
     * The bitwise encoding of the {@code lookup} action.
     */
    public static final int ACTION_LOOKUP               = 0b000001000;
    /**
     * The bitwise encoding of the {@code list} action.
     */
    public static final int ACTION_LIST                 = 0b000010000;
    /**
     * The bitwise encoding of the {@code listBindings} action.
     */
    public static final int ACTION_LIST_BINDINGS        = 0b000100000;
    /**
     * The bitwise encoding of the {@code createSubcontext} action.
     */
    public static final int ACTION_CREATE_SUBCONTEXT    = 0b001000000;
    /**
     * The bitwise encoding of the {@code destroySubcontext} action.
     */
    public static final int ACTION_DESTROY_SUBCONTEXT   = 0b010000000;
    /**
     * The bitwise encoding of the {@code addNamingListener} action.
     */
    public static final int ACTION_ADD_NAMING_LISTENER  = 0b100000000;
    /**
     * The bitwise encoding of the {@code *} action.  This value is the bitwise-OR of all of the other action bits.
     */
    public static final int ACTION_ALL                  = 0b111111111;

    /**
     * Construct a new instance.
     *
     * @param name the path name (must not be {@code null})
     * @param actions the actions (must not be {@code null})
     */
    public JndiPermission(final String name, final String actions) {
        this(name, parseActions(Assert.checkNotNullParam("actions", actions)));
    }

    /**
     * Construct a new instance using an action bitmask.  If a bit in the mask falls outside of {@link #ACTION_ALL}, it
     * is stripped.
     *
     * @param name the path name (must not be {@code null})
     * @param actionBits the action bits
     */
    public JndiPermission(final String name, final int actionBits) {
        super(canonicalize1(Assert.checkNotNullParam("name", name)));
        this.actionBits = actionBits & ACTION_ALL;
    }

    /**
     * Determine if this permission implies the other permission.
     *
     * @param permission the other permission
     * @return {@code true} if this permission implies the other, {@code false} if it does not or {@code permission} is {@code null}
     */
    public boolean implies(final Permission permission) {
        return permission instanceof JndiPermission && implies((JndiPermission) permission);
    }

    /**
     * Determine if this permission implies the other permission.
     *
     * @param permission the other permission
     * @return {@code true} if this permission implies the other, {@code false} if it does not or {@code permission} is {@code null}
     */
    public boolean implies(final JndiPermission permission) {
        return permission != null && ((actionBits & permission.actionBits) == permission.actionBits) && impliesPath(permission.getName());
    }

    /**
     * Determine if this permission implies the given {@code actions} on the given {@code name}.
     *
     * @param name the name (must not be {@code null})
     * @param actions the actions (must not be {@code null})
     * @return {@code true} if this permission implies the {@code actions} on the {@code name}, {@code false} otherwise
     */
    public boolean implies(final String name, final String actions) {
        return implies(name, parseActions(actions));
    }

    /**
     * Determine if this permission implies the given {@code actionsBits} on the given {@code name}.
     *
     * @param name the name (must not be {@code null})
     * @param actionBits the action bits
     * @return {@code true} if this permission implies the {@code actionBits} on the {@code name}, {@code false} otherwise
     */
    public boolean implies(final String name, final int actionBits) {
        Assert.checkNotNullParam("name", name);
        final int maskedBits = actionBits & ACTION_ALL;
        return (this.actionBits & maskedBits) == maskedBits && impliesPath(name);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof JndiPermission && equals((JndiPermission)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(JndiPermission other) {
        return this == other || other != null && getName().equals(other.getName()) && actionBits == other.actionBits;
    }

    /**
     * Get the hash code of this permission.
     *
     * @return the hash code of this permission
     */
    public int hashCode() {
        return actionBits * 23 + getName().hashCode();
    }

    /**
     * Get the actions string.  The actions string will be a canonical version of the one passed in at construction.
     *
     * @return the actions string (not {@code null})
     */
    public String getActions() {
        final String actionString = this.actionString;
        if (actionString != null) {
            return actionString;
        }
        int actionBits = this.actionBits;
        if (actionBits == ACTION_ALL) {
            return this.actionString = "*";
        }
        int m = Integer.lowestOneBit(actionBits);
        if (m != 0) {
            StringBuilder b = new StringBuilder();
            b.append(getAction(m));
            actionBits &= ~m;
            while (actionBits != 0) {
                m = Integer.lowestOneBit(actionBits);
                b.append(',').append(getAction(m));
                actionBits &= ~m;
            }
            return this.actionString = b.toString();
        } else {
            return this.actionString = "";
        }
    }

    /**
     * Get the action bits.
     *
     * @return the action bits
     */
    public int getActionBits() {
        return actionBits;
    }

    /**
     * Return a permission which is equal to this one except with its actions reset to {@code actionBits}.  If the given
     * {@code actionBits} equals the current bits of this permission, then this permission instance is returned; otherwise
     * a new permission is constructed. Any action bits which fall outside of {@link #ACTION_ALL} are silently ignored.
     *
     * @param actionBits the action bits to use
     * @return a permission with only the given action bits (not {@code null})
     */
    public JndiPermission withNewActions(int actionBits) {
        actionBits &= ACTION_ALL;
        if (actionBits == this.actionBits) {
            return this;
        } else {
            return new JndiPermission(getName(), actionBits);
        }
    }

    /**
     * Return a permission which is equal to this one except with its actions reset to {@code actions}.  If the given
     * {@code actions} equals the current actions of this permission, then this permission instance is returned; otherwise
     * a new permission is constructed.
     *
     * @param actions the actions to use (must not be {@code null})
     * @return a permission with only the given action bits (not {@code null})
     */
    public JndiPermission withNewActions(String actions) {
        return withNewActions(parseActions(Assert.checkNotNullParam("actions", actions)));
    }

    /**
     * Return a permission which is equal to this one except with additional action bits.  If the given {@code actionBits}
     * do not add any new actions, then this permission instance is returned; otherwise a new permission is constructed.
     * Any action bits which fall outside of {@link #ACTION_ALL} are silently ignored.
     *
     * @param actionBits the action bits to add
     * @return a permission with the union of permissions from this instance and the given bits (not {@code null})
     */
    public JndiPermission withActions(int actionBits) {
        return withNewActions(actionBits & ACTION_ALL | this.actionBits);
    }

    /**
     * Return a permission which is equal to this one except with additional actions.  If the given {@code actionBits}
     * do not add any new actions, then this permission instance is returned; otherwise a new permission is constructed.
     *
     * @param actions the actions to add (must not be {@code null})
     * @return a permission with the union of permissions from this instance and the given bits (not {@code null})
     */
    public JndiPermission withActions(String actions) {
        return withActions(parseActions(Assert.checkNotNullParam("actions", actions)));
    }

    /**
     * Return a permission which is equal to this one except without some action bits.  If the given {@code actionBits}
     * do not remove any actions, then this permission instance is returned; otherwise a new permission is constructed.
     * Any action bits which fall outside of {@link #ACTION_ALL} are silently ignored.
     *
     * @param actionBits the action bits to remove
     * @return a permission with the given bits subtracted from this instance (not {@code null})
     */
    public JndiPermission withoutActions(int actionBits) {
        return withNewActions(this.actionBits & ~(actionBits & ACTION_ALL));
    }

    /**
     * Return a permission which is equal to this one except without some actions.  If the given {@code actions}
     * do not remove any actions, then this permission instance is returned; otherwise a new permission is constructed.
     *
     * @param actions the actions to remove (must not be {@code null})
     * @return a permission with the given bits subtracted from this instance (not {@code null})
     */
    public JndiPermission withoutActions(String actions) {
        return withoutActions(parseActions(Assert.checkNotNullParam("actions", actions)));
    }

    /**
     * Construct a new type-specific permission collection.
     *
     * @return the new permission collection instance (not {@code null})
     */
    public PermissionCollection newPermissionCollection() {
        return new JndiPermissionCollection();
    }

    // semi-private

    Object writeReplace() {
        return new SerializedJndiPermission(getName(), getActions());
    }

    boolean impliesPath(final String yourName) {
        return yourName.startsWith("java:") ? impliesPath0(yourName.substring(5)) : impliesPath0(yourName);
    }

    // private

    private boolean impliesPath0(final String yourName) {
        // segment-by-segment comparison
        final String myName = getName();
        final Iterator<String> myIter = JndiPermissionNameParser.nameIterator(myName);
        final Iterator<String> yourIter = JndiPermissionNameParser.nameIterator(yourName);
        // even if it's just "", there is always a first element
        assert myIter.hasNext() && yourIter.hasNext();
        String myNext;
        String yourNext;
        for (;;) {
            myNext = myIter.next();
            yourNext = yourIter.next();
            if (myNext.equals("-")) {
                // "-" implies everything including ""
                return true;
            }
            if (! myNext.equals("*") && ! myNext.equals(yourNext)) {
                // "foo/bar" does not imply "foo/baz"
                return false;
            }
            if (myIter.hasNext()) {
                if (! yourIter.hasNext()) {
                    // "foo/bar" does not imply "foo"
                    return false;
                }
            } else {
                // if neither has next, "foo/bar" implies "foo/bar", else "foo" does not imply "foo/bar"
                return ! yourIter.hasNext();
            }
        }
    }

    private static String canonicalize1(String name) {
        Assert.checkNotNullParam("name", name);
        return name.equalsIgnoreCase("<<ALL BINDINGS>>") ? "-" : canonicalize2(name);
    }

    private static String canonicalize2(String name) {
        return name.startsWith("java:") ? name.substring(5) : name;
    }

    private static int parseActions(final String actionsString) {
        // TODO: switch to Elytron utility methods to do this
        int actions = 0;
        int pos = 0;
        int idx = actionsString.indexOf(',');
        for (;;) {
            String str;
            if (idx == -1) {
                str = actionsString.substring(pos, actionsString.length()).trim();
                if (! str.isEmpty()) actions |= parseAction(str);
                return actions;
            } else {
                str = actionsString.substring(pos, idx).trim();
                pos = idx + 1;
                if (! str.isEmpty()) actions |= parseAction(str);
                idx = actionsString.indexOf(',', pos);
            }
        }
    }

    private static int parseAction(final String str) {
        switch (str) {
            case "*":
            case "all": return ACTION_ALL;
            case "bind": return ACTION_BIND;
            case "rebind": return ACTION_REBIND;
            case "unbind": return ACTION_UNBIND;
            case "lookup": return ACTION_LOOKUP;
            case "list": return ACTION_LIST;
            case "listBindings": return ACTION_LIST_BINDINGS;
            case "createSubcontext": return ACTION_CREATE_SUBCONTEXT;
            case "destroySubcontext": return ACTION_DESTROY_SUBCONTEXT;
            case "addNamingListener": return ACTION_ADD_NAMING_LISTENER;
            default: {
                throw NamingLogger.ROOT_LOGGER.invalidPermissionAction(str);
            }
        }
    }

    private String getAction(final int bit) {
        switch (bit) {
            case ACTION_BIND: return "bind";
            case ACTION_REBIND: return "rebind";
            case ACTION_UNBIND: return "unbind";
            case ACTION_LOOKUP: return "lookup";
            case ACTION_LIST: return "list";
            case ACTION_LIST_BINDINGS: return "listBindings";
            case ACTION_CREATE_SUBCONTEXT: return "createSubcontext";
            case ACTION_DESTROY_SUBCONTEXT: return "destroySubcontext";
            case ACTION_ADD_NAMING_LISTENER: return "addNamingListener";
            default: throw Assert.impossibleSwitchCase(bit);
        }
    }
}

