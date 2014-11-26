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

package org.wildfly.iiop.openjdk.naming;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.Binding;
import org.omg.CosNaming.BindingIteratorHelper;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.BindingType;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextExtPOA;
import org.omg.CosNaming.NamingContextExtPackage.InvalidAddress;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotEmpty;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.CosNaming.NamingContextPackage.NotFoundReason;
import org.omg.PortableServer.POA;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * <p>
 * This class implements an in-VM CORBA Naming Server that caches for JBoss to use. All contexts keep a cache of the
 * local sub-contexts to avoid unnecessary remote calls when resolving a complex name.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CorbaNamingContext extends NamingContextExtPOA implements Serializable {

    private static final long serialVersionUID = 132820915998903191L;

    /**
     * static references to the running ORB and root POA.
     */
    private static ORB orb;
    private static POA rootPoa;

    /**
     * the naming service POA - i.e. the POA that activates all naming contexts.
     */
    private transient POA poa;

    /**
     * table of all name bindings in this contexts, ie. name -> obj ref.
     */
    private Map<Name, Object> names = new Hashtable<Name, Object>();

    /**
     * table of all subordinate naming contexts, ie. name -> obj ref.
     */
    private Map<Name, Object> contexts = new Hashtable<Name, Object>();

    /**
     * cache of all active naming context implementations - used when resolving contexts recursively to avoid
     * unnecessary remote calls that may lead to thread pool depletion.
     */
    private static Map<String, CorbaNamingContext> contextImpls = new Hashtable<String, CorbaNamingContext>();

    /**
     * no tests of bound objects for existence
     */
    private boolean noPing = false;

    /**
     * purge?
     */
    private boolean doPurge = false;

    private boolean destroyed = false;

    private int childCount = 0;

    //======================================= Initialization Methods ==================================//

    /**
     * <p>
     * This method needs to be called once to initialize the static fields orb and rootPoa.
     * </p>
     *
     * @param orb     a reference to the running {@code ORB} instance.
     * @param rootPoa a reference to the root {@code POA}.
     */
    public static void init(org.omg.CORBA.ORB orb, org.omg.PortableServer.POA rootPoa) {
        CorbaNamingContext.orb = orb;
        CorbaNamingContext.rootPoa = rootPoa;


    }

    /**
     * <p>
     * This method needs to be called for each newly created or re-read naming context to set its POA.
     * </p>
     *
     * @param poa     a reference to the Naming Service {@code POA}.
     * @param doPurge a boolean that indicates if dead objects should be purged from the context ({@code true}) upon cleanup
     *                or not ({@code false}).
     * @param noPing  a boolean that indicates if the method {@code resolve} should check if the resolved name or context is
     *                alive before returning it. If {@code false}, the name will be checked (default)
     */
    public void init(POA poa, boolean doPurge, boolean noPing) {
        this.poa = poa;
        this.doPurge = doPurge;
        this.noPing = noPing;
    }

    //======================================= NamingContextOperation Methods ==================================//

    public void bind(NameComponent[] nc, org.omg.CORBA.Object obj) throws NotFound, CannotProceed, InvalidName,
            AlreadyBound {
        if (this.destroyed)
            throw new CannotProceed();

        if (nc == null || nc.length == 0)
            throw new InvalidName();

        if (obj == null)
            throw new org.omg.CORBA.BAD_PARAM();

        Name n = new Name(nc);
        Name ctx = n.ctxName();
        NameComponent nb = n.baseNameComponent();

        if (ctx == null) {
            if (this.names.containsKey(n)) {
                // if the name is still in use, try to ping the object
                org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.names.get(n);
                if (isDead(ref)) {
                    rebind(n.components(), obj);
                    return;
                }
                throw new AlreadyBound();
            } else if (this.contexts.containsKey(n)) {
                // if the name is still in use, try to ping the object
                org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.contexts.get(n);
                if (isDead(ref))
                    unbind(n.components());
                throw new AlreadyBound();
            }

            if ((this.names.put(n, obj)) != null)
                throw new CannotProceed(_this(), n.components());

            IIOPLogger.ROOT_LOGGER.debugf("Bound name: %s", n);
        } else {
            NameComponent[] ncx = new NameComponent[]{nb};
            org.omg.CORBA.Object context = this.resolve(ctx.components());

            // try first to call the context implementation object directly.
            String contextOID = this.getObjectOID(context);
            CorbaNamingContext jbossContext = (contextOID == null ? null : contextImpls.get(contextOID));
            if (jbossContext != null)
                jbossContext.bind(ncx, obj);
            else
                NamingContextExtHelper.narrow(context).bind(ncx, obj);
        }
    }

    public void bind_context(NameComponent[] nc, NamingContext obj) throws NotFound, CannotProceed, InvalidName,
            AlreadyBound {
        if (this.destroyed)
            throw new CannotProceed();

        Name n = new Name(nc);
        Name ctx = n.ctxName();
        NameComponent nb = n.baseNameComponent();

        if (ctx == null) {
            if (this.names.containsKey(n)) {
                // if the name is still in use, try to ping the object
                org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.names.get(n);
                if (isDead(ref))
                    unbind(n.components());
                else
                    throw new AlreadyBound();
            } else if (this.contexts.containsKey(n)) {
                // if the name is still in use, try to ping the object
                org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.contexts.get(n);
                if (isDead(ref)) {
                    rebind_context(n.components(), obj);
                    return;
                }
                throw new AlreadyBound();
            }

            if ((this.contexts.put(n, obj)) != null)
                throw new CannotProceed(_this(), n.components());

            IIOPLogger.ROOT_LOGGER.debugf("Bound context: %s", n);
        } else {
            NameComponent[] ncx = new NameComponent[]{nb};
            org.omg.CORBA.Object context = this.resolve(ctx.components());

            // try first to call the context implementation object directly.
            String contextOID = this.getObjectOID(context);
            CorbaNamingContext jbossContext = (contextOID == null ? null : contextImpls.get(contextOID));
            if (jbossContext != null)
                jbossContext.bind_context(ncx, obj);
            else
                NamingContextExtHelper.narrow(context).bind_context(ncx, obj);
        }
    }

    public NamingContext bind_new_context(NameComponent[] nc) throws NotFound, CannotProceed, InvalidName, AlreadyBound {
        if (this.destroyed)
            throw new CannotProceed();

        if (nc == null || nc.length == 0)
            throw new InvalidName();

        NamingContext context = new_context();
        if (context == null)
            throw new CannotProceed();

        bind_context(nc, context);
        return context;
    }

    public void destroy() throws NotEmpty {
        if (this.destroyed)
            return;

        if (!this.names.isEmpty() || !this.contexts.isEmpty())
            throw new NotEmpty();
        else {
            this.names = null;
            this.contexts = null;
            this.destroyed = true;
        }
    }

    public void list(int how_many, BindingListHolder bl, BindingIteratorHolder bi) {
        if (this.destroyed)
            return;

        Binding[] result;
        this.cleanup();

        int size = how_many();

        Iterator<Name> names = this.names.keySet().iterator();
        Iterator<Name> contexts = this.contexts.keySet().iterator();

        if (how_many < size) {
            // counter for copies
            int how_many_ctr = how_many;

            // set up an array with "how_many" bindings
            result = new Binding[how_many];
            for (; names.hasNext() && how_many_ctr > 0; how_many_ctr--)
                result[how_many_ctr - 1] = new Binding((names.next()).components(), BindingType.nobject);

            for (; contexts.hasNext() && how_many_ctr > 0; how_many_ctr--)
                result[how_many_ctr - 1] = new Binding((contexts.next()).components(), BindingType.ncontext);

            // create a new BindingIterator for the remaining arrays
            size -= how_many;
            Binding[] rest = new Binding[size];
            for (; names.hasNext() && size > 0; size--)
                rest[size - 1] = new Binding((names.next()).components(), BindingType.nobject);

            for (; contexts.hasNext() && size > 0; size--)
                rest[size - 1] = new Binding((contexts.next()).components(), BindingType.ncontext);

            org.omg.CORBA.Object o;
            try {
                // Iterators are activated with the RootPOA (transient)
                byte[] oid = rootPoa.activate_object(new BindingIteratorImpl(rest));
                o = rootPoa.id_to_reference(oid);
            } catch (Exception e) {
                IIOPLogger.ROOT_LOGGER.logInternalError(e);
                throw new INTERNAL(e.toString());
            }

            bi.value = BindingIteratorHelper.narrow(o);
        } else {
            result = new Binding[size];
            for (; names.hasNext() && size > 0; size--)
                result[size - 1] = new Binding((names.next()).components(), BindingType.nobject);

            for (; contexts.hasNext() && size > 0; size--)
                result[size - 1] = new Binding((contexts.next()).components(), BindingType.ncontext);
        }

        bl.value = result;
    }

    public NamingContext new_context() {
        try {
            // create and initialize a new context.
            CorbaNamingContext newContextImpl = new CorbaNamingContext();
            newContextImpl.init(this.poa, this.doPurge, this.noPing);
            // create the oid for the new context and activate it with the naming service POA.
            String oid = new String(this.poa.servant_to_id(this)) + "/ctx" + (++this.childCount);
            this.poa.activate_object_with_id(oid.getBytes(), newContextImpl);
            // add the newly-created context to the cache.
            contextImpls.put(oid, newContextImpl);
            return NamingContextExtHelper.narrow(this.poa.create_reference_with_id(oid.getBytes(),
                    "IDL:omg.org/CosNaming/NamingContextExt:1.0"));
        } catch (Exception e) {
            IIOPLogger.ROOT_LOGGER.failedToCreateNamingContext(e);
            return null;
        }
    }

    public void rebind(NameComponent[] nc, org.omg.CORBA.Object obj) throws NotFound, CannotProceed, InvalidName {
        if (this.destroyed)
            throw new CannotProceed();

        if (nc == null || nc.length == 0)
            throw new InvalidName();

        if (obj == null)
            throw new org.omg.CORBA.BAD_PARAM();

        Name n = new Name(nc);
        Name ctx = n.ctxName();
        NameComponent nb = n.baseNameComponent();

        if (ctx == null) {
            // the name is bound, but it is bound to a context - the client should have been using rebind_context!
            if (this.contexts.containsKey(n))
                throw new NotFound(NotFoundReason.not_object, new NameComponent[]{nb});

            // try remove an existing binding.
            org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.names.remove(n);
            if (ref != null)
                ref._release();

            // do the rebinding in this context
            this.names.put(n, obj);
            IIOPLogger.ROOT_LOGGER.debugf("Bound name: %s", n);
        } else {
            // rebind in the correct context
            NameComponent[] ncx = new NameComponent[]{nb};
            org.omg.CORBA.Object context = this.resolve(ctx.components());

            // try first to call the context implementation object directly.
            String contextOID = this.getObjectOID(context);
            CorbaNamingContext jbossContext = (contextOID == null ? null : contextImpls.get(contextOID));
            if (jbossContext != null)
                jbossContext.rebind(ncx, obj);
            else
                NamingContextExtHelper.narrow(context).rebind(ncx, obj);
        }
    }

    public void rebind_context(NameComponent[] nc, NamingContext obj) throws NotFound, CannotProceed, InvalidName {
        if (this.destroyed)
            throw new CannotProceed();

        if (nc == null || nc.length == 0)
            throw new InvalidName();

        if (obj == null)
            throw new org.omg.CORBA.BAD_PARAM();

        Name n = new Name(nc);
        Name ctx = n.ctxName();
        NameComponent nb = n.baseNameComponent();

        if (ctx == null) {
            // the name is bound, but it is bound to an object - the client should have been using rebind().
            if (this.names.containsKey(n))
                throw new NotFound(NotFoundReason.not_context, new NameComponent[]{nb});

            // try to remove an existing context binding.
            org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.contexts.remove(n);
            if (ref != null) {
                ref._release();
                // remove the old context from the implementation cache.
                String oid = this.getObjectOID(ref);
                if (oid != null)
                    contextImpls.remove(oid);
            }

            this.contexts.put(n, obj);
            IIOPLogger.ROOT_LOGGER.debugf("Bound context: %s", n.baseNameComponent().id);
        } else {
            // rebind in the correct context
            NameComponent[] ncx = new NameComponent[]{nb};
            org.omg.CORBA.Object context = this.resolve(ctx.components());

            // try first to call the context implementation object directly.
            String contextOID = this.getObjectOID(context);
            CorbaNamingContext jbossContext = (contextOID == null ? null : contextImpls.get(contextOID));
            if (jbossContext != null)
                jbossContext.rebind_context(ncx, obj);
            else
                NamingContextExtHelper.narrow(context).rebind_context(ncx, obj);
        }
    }

    public org.omg.CORBA.Object resolve(NameComponent[] nc) throws NotFound, CannotProceed, InvalidName {
        if (this.destroyed)
            throw new CannotProceed();

        if (nc == null || nc.length == 0)
            throw new InvalidName();

        Name n = new Name(nc[0]);
        if (nc.length > 1) {
            org.omg.CORBA.Object next_context = (org.omg.CORBA.Object) this.contexts.get(n);
            if ((next_context == null) || (isDead(next_context)))
                throw new NotFound(NotFoundReason.missing_node, nc);

            NameComponent[] nc_prime = new NameComponent[nc.length - 1];
            System.arraycopy(nc, 1, nc_prime, 0, nc_prime.length);

            // try first to call the context implementation object directly.
            String contextOID = this.getObjectOID(next_context);
            CorbaNamingContext jbossContext = (contextOID == null ? null : contextImpls.get(contextOID));
            if (jbossContext != null)
                return jbossContext.resolve(nc_prime);
            else
                return NamingContextExtHelper.narrow(next_context).resolve(nc_prime);
        } else {
            org.omg.CORBA.Object result = (org.omg.CORBA.Object) this.contexts.get(n);

            if (result == null)
                result = (org.omg.CORBA.Object) this.names.get(n);

            if (result == null)
                throw new NotFound(NotFoundReason.missing_node, n.components());

            if (!noPing && isDead(result))
                throw new NotFound(NotFoundReason.missing_node, n.components());

            return result;
        }
    }

    public void unbind(NameComponent[] nc) throws NotFound, CannotProceed, InvalidName {
        if (this.destroyed)
            throw new CannotProceed();

        if (nc == null || nc.length == 0)
            throw new InvalidName();

        Name n = new Name(nc);
        Name ctx = n.ctxName();
        NameComponent nb = n.baseNameComponent();

        if (ctx == null) {
            if (this.names.containsKey(n)) {
                org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.names.remove(n);
                ref._release();
                IIOPLogger.ROOT_LOGGER.debugf("Unbound: %s", n);
            } else if (this.contexts.containsKey(n)) {
                org.omg.CORBA.Object ref = (org.omg.CORBA.Object) this.contexts.remove(n);
                ref._release();
                // remove the context from the implementation cache.
                String oid = this.getObjectOID(ref);
                if (oid != null)
                    contextImpls.remove(oid);

                IIOPLogger.ROOT_LOGGER.debugf("Unbound: %s", n);
            } else {
                IIOPLogger.ROOT_LOGGER.failedToUnbindObject(n);
                throw new NotFound(NotFoundReason.not_context, n.components());
            }
        } else {
            NameComponent[] ncx = new NameComponent[]{nb};
            org.omg.CORBA.Object context = this.resolve(ctx.components());

            // try first to call the context implementation object directly.
            String contextOID = this.getObjectOID(context);
            CorbaNamingContext jbossContext = (contextOID == null ? null : contextImpls.get(contextOID));
            if (jbossContext != null)
                jbossContext.unbind(ncx);
            else
                NamingContextExtHelper.narrow(context).unbind(ncx);
        }
    }

    //======================================= NamingContextExtOperations Methods ==================================//

    public org.omg.CORBA.Object resolve_str(String n) throws NotFound, CannotProceed, InvalidName {
        return resolve(to_name(n));
    }

    public NameComponent[] to_name(String sn) throws InvalidName {
        return Name.toName(sn);
    }

    public String to_string(NameComponent[] n) throws InvalidName {
        return Name.toString(n);
    }

    public String to_url(String addr, String sn) throws InvalidAddress, InvalidName {
        return null;
    }

    //======================================= Private Helper Methods ==================================//

    /**
     * <p>
     * Cleanup bindings, i.e. ping every object and remove bindings to non-existent objects.
     * </p>
     */
    private void cleanup() {
        // Check if object purging enabled
        if (!this.doPurge)
            return;

        for (Name key : this.names.keySet()) {
            if (isDead(((org.omg.CORBA.Object) this.names.get(key)))) {
                this.names.remove(key);
            }
        }

        for (Name key : this.contexts.keySet()) {
            org.omg.CORBA.Object object = (org.omg.CORBA.Object) this.contexts.get(key);
            if (isDead(object)) {
                this.contexts.remove(key);
                String oid = this.getObjectOID(object);
                if (oid != null)
                    contextImpls.remove(oid);
            }
        }
    }

    /**
     * <p>
     * Obtains the OID of the specified CORBA object.
     * </p>
     *
     * @param object the CORBA object whose OID is to be extracted.
     * @return a {@code String} representing the object OID or null if the method is unable to obtain the object OID.
     */
    private String getObjectOID(org.omg.CORBA.Object object) {
        String oid = null;
        try {
            byte[] oidBytes = this.poa.reference_to_id(object);
            if (oidBytes != null)
                oid = new String(oidBytes);
        } catch (Exception e) {
            IIOPLogger.ROOT_LOGGER.debug("Unable to obtain id from object", e);
        }
        return oid;
    }

    /**
     * <p>
     * Obtains the number of bindings in this context.
     * </p>
     *
     * @return the number of bindings in this context
     */
    private int how_many() {
        if (this.destroyed)
            return 0;
        return this.names.size() + this.contexts.size();
    }

    /**
     * <p>
     * Determines if the supplied object is non_existent
     * </p>
     *
     * @param o the CORBA object being verified.
     * @return {@code true} if the object is non-existent; {@code false} otherwise.
     */
    private boolean isDead(org.omg.CORBA.Object o) {
        boolean non_exist;
        try {
            non_exist = o._non_existent();
        } catch (org.omg.CORBA.SystemException e) {
            non_exist = true;
        }
        return non_exist;
    }

    /**
     * <p>
     * Overrides readObject in Serializable.
     * </p>
     *
     * @param in the {@code InputStream} used to read the objects.
     * @throws Exception if an error occurs while reading the objects from the stream.
     */
    private void readObject(ObjectInputStream in) throws Exception {
        in.defaultReadObject();

        /**
         * Recreate tables. For serialization, object references have been transformed into strings
         */
        for (Name key : this.contexts.keySet()) {
            String ref = (String) this.contexts.remove(key);
            this.contexts.put(key, orb.string_to_object(ref));
        }

        for (Name key : this.names.keySet()) {
            String ref = (String) this.names.remove(key);
            this.names.put(key, orb.string_to_object(ref));
        }
    }

    /**
     * <p>
     * Overrides writeObject in Serializable.
     * </p>
     *
     * @param out the {@code OutputStream} where the objects will be written.
     * @throws IOException if an error occurs while writing the objects to the stream.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        /*
        * For serialization, object references are transformed into strings
        */
        for (Name key : this.contexts.keySet()) {
            org.omg.CORBA.Object o = (org.omg.CORBA.Object) this.contexts.remove(key);
            this.contexts.put(key, orb.object_to_string(o));
        }

        for (Name key : this.names.keySet()) {
            org.omg.CORBA.Object o = (org.omg.CORBA.Object) this.names.remove(key);
            this.names.put(key, orb.object_to_string(o));
        }

        out.defaultWriteObject();
    }
}