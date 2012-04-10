/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import static org.jboss.as.naming.NamingMessages.MESSAGES;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.naming.Name;

/**
 * This class represents access to a path in the JNDI tree. A JndiPermission
 * consists of a pathname and a set of actions valid for that pathname.
 * <p/>
 * Pathname is the pathname of the file or directory granted the specified
 * actions. A pathname that ends in "/*" indicates all the files and directories
 * contained in that directory. A pathname that ends with "/-" indicates
 * (recursively) all files and subdirectories contained in that directory. A
 * pathname consisting of the special token "&lt;&lt;ALL BINDINGS&gt;&gt;" matches
 * <b>any</b> file.
 * <p/>
 * The actions to be granted are passed to the constructor in an array of
 * {@code Action} instances.  The possible actions are "bind", "rebind",
 * "unbind", "lookup", "list", "listBindings", and "createSubcontext".
 * Their meaning is defined as follows:
 * <p/>
 * <DL>
 * <DT> bind
 * <DD> Context.bind permission
 * <DT> rebind
 * <DD> Context.rebind permission
 * <DT> unbind
 * <DD> Context.unbind permission.
 * <DT> lookup
 * <DD> Context.lookup permission.
 * <DT> list
 * <DD> Context.list permission.
 * <DT> listBindings
 * <DD> Context.listBindings permission.
 * <DT> createSubcontext
 * <DD> Context.createSubcontext permission.
 * </DL>
 * <p/>
 * Be careful when granting JndiPermissions. Think about the implications of
 * granting read and especially write access to various files and directories.
 * The "&lt;&lt;ALL BINDINGS>>" permission with write action is especially
 * dangerous. This grants permission to write to the entire file system. One
 * thing this effectively allows is replacement of the system binary, including
 * the JVM runtime environment.
 * <p/>
 * <p/>
 * Please note: Code can always read a file from the same directory it's in (or
 * a subdirectory of that directory); it does not need explicit permission to do
 * so.
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81310 $
 * @serial exclude
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
public final class JndiPermission extends Permission
        implements Serializable {
    private static final long serialVersionUID = 1;

    public enum Action {
        NONE("none", 0x0),
        BIND("bind", 1),
        REBIND("rebind", 2),
        UNBIND("unbind", 4),
        LOOKUP("lookup", 8),
        LIST("list", 16),
        LIST_BINDINGS("listBindings", 32),
        CREATE_SUBCONTEXT("createSubcontext", 64),
        ALL("all", BIND.mask | REBIND.mask | UNBIND.mask | LOOKUP.mask | LIST.mask | LIST_BINDINGS.mask | CREATE_SUBCONTEXT.mask);

        private String actionName;
        private int mask;

        private Action(String actionName, int mask) {
            this.actionName = actionName;
            this.mask = mask;
        }

        public static Action forName(final String actionName) {
            for(Action action : Action.values()) {
                if(action.actionName.equals(actionName))
                    return action;
            }
            return null;
        }
    }

    // the actions mask
    private transient int mask;

    // does path indicate a directory? (wildcard or recursive)
    private transient boolean directory;

    // is it a recursive directory specification?
    private transient boolean recursive;

    /**
     * the actions string.
     *
     * @serial
     */
    private String actions; // Left null as long as possible, then

    // created and re-used in the getAction function.

    // canonicalized dir path. In the case of
    // directories, it is the name "/blah/*" or "/blah/-" without
    // the last character (the "*" or "-").

    private transient String cpath;

    // static Strings used by init(int mask)
    private static final char RECURSIVE_CHAR = '-';

    private static final char WILD_CHAR = '*';

    /**
     * initialize a JndiPermission object. Common to all constructors. Also
     * called during de-serialization.
     *
     * @param mask the actions mask to use.
     */
    private void init(int mask) {
        if ((mask & Action.ALL.mask) != mask)
            throw MESSAGES.invalidActionMask();

        if (mask == Action.NONE.mask)
            throw MESSAGES.invalidActionMask();

        if ((cpath = getName()) == null)
            throw new NullPointerException(MESSAGES.cannotBeNull("name"));

        this.mask = mask;

        if (cpath.equals("<<ALL BINDINGS>>")) {
            directory = true;
            recursive = true;
            cpath = "";
            return;
        }

        int len = cpath.length();
        char last = ((len > 0) ? cpath.charAt(len - 1) : 0);

        if (last == RECURSIVE_CHAR && cpath.charAt(len - 2) == '/') {
            directory = true;
            recursive = true;
            cpath = cpath.substring(0, --len);
        } else if (last == WILD_CHAR && cpath.charAt(len - 2) == '/') {
            directory = true;
            // recursive = false;
            cpath = cpath.substring(0, --len);
        } else {
            // overkill since they are initialized to false, but
            // commented out here to remind us...
            // directory = false;
            // recursive = false;
        }
    }

    /**
     * Creates a new JndiPermission object with the specified actions. <i>path</i>
     * is the pathname of a file or directory, and <i>actions</i> contains a
     * comma-separated list of the desired actions granted on the file or
     * directory. Possible actions are "bind", "rebind", "unbind", "lookup",
     * "list", "listBindings", and "createSubcontext".
     * <p/>
     * <p/>
     * A pathname that ends in "/*" (where "/" is the file separator character,
     * <code>'/'</code>) indicates all the files and directories contained in
     * that directory. A pathname that ends with "/-" indicates (recursively) all
     * files and subdirectories contained in that directory. The special pathname
     * "&lt;&lt;ALL BINDINGS&gt;&gt;" matches any file.
     * <p/>
     * <p/>
     * A pathname consisting of a single "*" indicates all the files in the
     * current directory, while a pathname consisting of a single "-" indicates
     * all the files in the current directory and (recursively) all files and
     * subdirectories contained in the current directory.
     * <p/>
     * <p/>
     * A pathname containing an empty string represents an empty path.
     *
     * @param path    the pathname of the file/directory.
     * @param actions the action string.
     * @throws IllegalArgumentException If actions is <code>null</code>, empty or contains an action
     *                                  other than the specified possible actions.
     */

    public JndiPermission(String path, Action... actions) {
        super(path);
        init(getMask(actions));
    }

    public JndiPermission(Name path, Action... actions) {
        this(path.toString(), actions);
    }

    /**
     * Checks if this JndiPermission object "implies" the specified permission.
     * <p/>
     * More specifically, this method returns true if:
     * <p/>
     * <ul>
     * <li> <i>p</i> is an instanceof JndiPermission,
     * <p/>
     * <li> <i>p</i>'s actions are a proper subset of this object's actions, and
     * <p/>
     * <li> <i>p</i>'s pathname is implied by this object's pathname. For
     * example, "/tmp/*" implies "/tmp/foo", since "/tmp/*" encompasses all files
     * in the "/tmp" directory, including the one named "foo".
     * </ul>
     *
     * @param p the permission to check against.
     * @return <code>true</code> if the specified permission is not
     *         <code>null</code> and is implied by this object,
     *         <code>false</code> otherwise.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof JndiPermission))
            return false;

        JndiPermission that = (JndiPermission) p;

        // we get the effective mask. i.e., the "and" of this and that.
        // They must be equal to that.mask for implies to return true.

        return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
    }

    /**
     * Checks if the Permission's actions are a proper subset of the this
     * object's actions. Returns the effective mask iff the this JndiPermission's
     * path also implies that JndiPermission's path.
     *
     * @param that the JndiPermission to check against.
     * @return the effective mask
     */
    boolean impliesIgnoreMask(JndiPermission that) {
        if (this.directory) {
            if (this.recursive) {
                // make sure that.path is longer then path so
                // something like /foo/- does not imply /foo
                if (that.directory) {
                    return (that.cpath.length() >= this.cpath.length())
                            && that.cpath.startsWith(this.cpath);
                } else {
                    return ((that.cpath.length() >= this.cpath.length()) && that.cpath
                            .startsWith(this.cpath));
                }
            } else {
                if (that.directory) {
                    // if the permission passed in is a directory
                    // specification, make sure that a non-recursive
                    // permission (i.e., this object) can't imply a recursive
                    // permission.
                    if (that.recursive)
                        return false;
                    else
                        return (this.cpath.equals(that.cpath));
                } else {
                    int last = that.cpath.lastIndexOf('/');
                    if (last == -1)
                        return false;
                    else {
                        // this.cpath.equals(that.cpath.substring(0, last+1));
                        // Use regionMatches to avoid creating new string
                        return (this.cpath.length() == (last + 1))
                                && this.cpath.regionMatches(0, that.cpath, 0, last + 1);
                    }
                }
            }
        } else if (that.directory) {
            // if this is NOT recursive/wildcarded,
            // do not let it imply a recursive/wildcarded permission
            return false;
        } else {
            return (this.cpath.equals(that.cpath));
        }
    }

    /**
     * Checks two JndiPermission objects for equality. Checks that <i>obj</i> is
     * a JndiPermission, and has the same pathname and actions as this object.
     * <p/>
     *
     * @param obj the object we are testing for equality with this object.
     * @return <code>true</code> if obj is a JndiPermission, and has the same
     *         pathname and actions as this JndiPermission object,
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof JndiPermission))
            return false;

        JndiPermission that = (JndiPermission) obj;

        return (this.mask == that.mask) && this.cpath.equals(that.cpath)
                && (this.directory == that.directory)
                && (this.recursive == that.recursive);
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */

    public int hashCode() {
        return this.cpath.hashCode();
    }

    /**
     * Converts an actions array to an actions mask.
     *
     * @param actions - an array of actions
     * @return the actions mask.
     */
    private static int getMask(final Action[] actions) {

        int mask = Action.NONE.mask;

        // Null action valid?
        if (actions == null || actions.length == 0) {
            return mask;
        }
        if(actions.length == 1) {
            return actions[0].mask;
        }

        for (Action action : actions) {
            mask |= action.mask;
        }

        return mask;
    }

    /**
     * Converts an actions String to an actions mask.
     *
     * @param actions - the comma separated list of actions.
     * @return the actions mask.
     */
    private static int getMask(String actions) {

        int mask = Action.NONE.mask;

        // Null action valid?
        if (actions == null || actions.length() == 0) {
            return mask;
        }
        // Check against use of constants
        Action action = Action.forName(actions);
        if(action != null) {
            return action.mask;
        }

        String[] sa = actions.split(",");
        for (String s : sa) {
            String key = s.toLowerCase(Locale.ENGLISH);
            action = Action.forName(key);
            if (action == null) {
                throw MESSAGES.invalidPermissionAction(s);
            }
            int i = action.mask;
            mask |= i;
        }

        return mask;
    }

    /**
     * Return the current action mask. Used by the JndiPermissionCollection.
     *
     * @return the actions mask.
     */

    int getMask() {
        return mask;
    }

    /**
     * Return the canonical string representation of the actions. Always returns
     * present actions in the following order: bind, rebind, unbind, lookup,
     * list, listBindings, createSubcontext
     *
     * @return the canonical string representation of the actions.
     */
    private static String getActions(int mask) {
        StringBuilder sb = new StringBuilder();
        boolean insertComma = false;
        final Action[] allActions = Action.values();
        for (int n = 0; n < allActions.length; n++) {
            int action = 1 << n;
            if ((mask & action) == action) {
                if (insertComma)
                    sb.append(',');
                sb.append(allActions[n].actionName);
                insertComma = true;
            }
        }

        return sb.toString();
    }

    /**
     * Returns the "canonical string representation" of the actions. That is,
     * this method always returns present actions in the following order: bind,
     * rebind, unbind, lookup, list, listBindings, createSubcontext.
     * For example, if this JndiPermission object allows
     * both unbind and bind actions, a call to <code>getActions</code> will
     * return the string "bind,unbind".
     *
     * @return the canonical string representation of the actions.
     */
    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }

    /**
     * Returns a new PermissionCollection object for storing JndiPermission
     * objects.
     * <p/>
     * JndiPermission objects must be stored in a manner that allows them to be
     * inserted into the collection in any order, but that also enables the
     * PermissionCollection <code>implies</code> method to be implemented in an
     * efficient (and consistent) manner.
     * <p/>
     * <p/>
     * For example, if you have two JndiPermissions:
     * <OL>
     * <LI> <code>"/tmp/-", "bind"</code>
     * <LI> <code>"/tmp/scratch/foo", "unbind"</code>
     * </OL>
     * <p/>
     * <p/>
     * and you are calling the <code>implies</code> method with the
     * JndiPermission:
     * <p/>
     * <pre>
     *   &quot;/tmp/scratch/foo&quot;, &quot;bind,unbind&quot;,
     * </pre>
     * <p/>
     * then the <code>implies</code> function must take into account both the
     * "/tmp/-" and "/tmp/scratch/foo" permissions, so the effective permission
     * is "bind,unbind", and <code>implies</code> returns true. The "implies"
     * semantics for JndiPermissions are handled properly by the
     * PermissionCollection object returned by this
     * <code>newPermissionCollection</code> method.
     *
     * @return a new PermissionCollection object suitable for storing
     *         JndiPermissions.
     */

    public PermissionCollection newPermissionCollection() {
        return new JndiPermissionCollection();
    }

    /**
     * WriteObject is called to save the state of the JndiPermission to a stream.
     * The actions are serialized, and the superclass takes care of the name.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }

    /**
     * readObject is called to restore the state of the JndiPermission from a
     * stream.
     */
    private void readObject(ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        // Read in the actions, then restore everything else by calling init.
        s.defaultReadObject();
        init(getMask(actions));
    }
}

/**
 * A JndiPermissionCollection stores a set of JndiPermission permissions.
 * JndiPermission objects must be stored in a manner that allows them to be
 * inserted in any order, but enable the implies function to evaluate the
 * implies method. For example, if you have two JndiPermissions:
 * <OL>
 * <LI> "/tmp/-", "bind"
 * <LI> "/tmp/scratch/foo", "unbind"
 * </OL>
 * And you are calling the implies function with the JndiPermission:
 * "/tmp/scratch/foo", "bind,unbind", then the implies function must take into
 * account both the /tmp/- and /tmp/scratch/foo permissions, so the effective
 * permission is "bind,unbind".
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81310 $
 * @serial include
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
final class JndiPermissionCollection extends PermissionCollection implements
        Serializable {
    private static final long serialVersionUID = 1;
    private List<JndiPermission> perms;

    /**
     * Create an empty JndiPermissions object.
     */
    public JndiPermissionCollection() {
        perms = new ArrayList<JndiPermission>();
    }

    /**
     * Adds a permission to the JndiPermissions. The key for the hash is
     * permission.path.
     *
     * @param permission the Permission object to add.
     * @throws IllegalArgumentException -
     *                                  if the permission is not a JndiPermission
     * @throws SecurityException        -
     *                                  if this JndiPermissionCollection object has been marked
     *                                  readonly
     */

    public void add(Permission permission) {
        if (!(permission instanceof JndiPermission))
            throw MESSAGES.invalidPermission(permission);
        if (isReadOnly())
            throw MESSAGES.cannotAddToReadOnlyPermissionCollection();

        synchronized (this) {
            perms.add((JndiPermission) permission);
        }
    }

    /**
     * Check and see if this set of permissions implies the permissions expressed
     * in "permission".
     *
     * @param permission the Permission object to compare
     * @return true if "permission" is a proper subset of a permission in the
     *         set, false if not.
     */

    public boolean implies(Permission permission) {
        if (!(permission instanceof JndiPermission))
            return false;

        JndiPermission fp = (JndiPermission) permission;

        int desired = fp.getMask();
        int effective = 0;
        int needed = desired;

        synchronized (this) {
            for (JndiPermission x : perms) {
                if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(fp)) {
                    effective |= x.getMask();
                    if ((effective & desired) == desired)
                        return true;
                    needed = (desired ^ effective);
                }
            }
        }
        return false;
    }

    /**
     * Returns an enumeration of all the JndiPermission objects in the container.
     *
     * @return an enumeration of all the JndiPermission objects.
     */

    public Enumeration elements() {
        // Convert Iterator into Enumeration
        synchronized (this) {
            return Collections.enumeration(perms);
        }
    }

}
