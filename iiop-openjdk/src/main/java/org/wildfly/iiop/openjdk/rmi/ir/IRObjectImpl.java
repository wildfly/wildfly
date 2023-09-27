/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi.ir;

import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.IRObjectOperations;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Abstract base class for all IR object implementations.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
abstract class IRObjectImpl implements IRObjectOperations {

    protected RepositoryImpl repository;
    protected final DefinitionKind def_kind;

    IRObjectImpl(DefinitionKind def_kind, RepositoryImpl repository) {
        this.def_kind = def_kind;
        this.repository = repository;
    }


    public DefinitionKind def_kind() {
        return def_kind;
    }

    public void destroy() {
        throw IIOPLogger.ROOT_LOGGER.cannotDestroyRMIIIOPMapping();
    }

    public abstract IRObject getReference();

    public void allDone()
            throws IRConstructionException {
        getReference();
    }

    /**
     * Unexport this object.
     */
    public void shutdown() {
        POA poa = getPOA();
        try {
            poa.deactivate_object(poa.reference_to_id(getReference()));
        } catch (UserException ex) {
            IIOPLogger.ROOT_LOGGER.warnCouldNotDeactivateIRObject(ex);
        }
    }

    public RepositoryImpl getRepository() {
        return repository;
    }

    /**
     * Return the ORB for this IRObject.
     */
    protected ORB getORB() {
        return repository.orb;
    }

    /**
     * Return the POA for this IRObject.
     */
    protected POA getPOA() {
        return repository.poa;
    }

    /**
     * Return the POA object ID of this IR object.
     */
    protected abstract byte[] getObjectId();

    /**
     * Convert a servant to a reference.
     */
    protected org.omg.CORBA.Object servantToReference(Servant servant) {
        byte[] id = getObjectId();

        try {
            repository.poa.activate_object_with_id(id, servant);
            org.omg.CORBA.Object ref = repository.poa.id_to_reference(id);
            return ref;
        } catch (WrongPolicy ex) {
            IIOPLogger.ROOT_LOGGER.debug("Exception converting CORBA servant to reference", ex);
        } catch (ServantAlreadyActive ex) {
            IIOPLogger.ROOT_LOGGER.debug("Exception converting CORBA servant to reference", ex);
        } catch (ObjectAlreadyActive ex) {
            IIOPLogger.ROOT_LOGGER.debug("Exception converting CORBA servant to reference", ex);
        } catch (ObjectNotActive ex) {
            IIOPLogger.ROOT_LOGGER.debug("Exception converting CORBA servant to reference", ex);
        }
        return null;
    }

}
