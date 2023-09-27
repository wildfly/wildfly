/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi.ir;

import java.nio.charset.StandardCharsets;

import org.omg.CORBA.ContainedPackage.Description;
import org.omg.CORBA.Container;
import org.omg.CORBA.ContainerHelper;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.Repository;
import org.omg.CORBA.RepositoryHelper;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;


/**
 * Abstract base class for all contained IR entities.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
abstract class ContainedImpl extends IRObjectImpl implements LocalContained {

    ContainedImpl(String id, String name, String version,
                  LocalContainer defined_in,
                  DefinitionKind def_kind, RepositoryImpl repository) {
        super(def_kind, repository);
        this.id = id;
        this.name = name;
        this.version = version;
        this.defined_in = defined_in;

        if (defined_in instanceof LocalContained)
            this.absolute_name = ((LocalContained) defined_in).absolute_name() +
                    "::" + name;
        else // must be Repository
            this.absolute_name = "::" + name;
    }

    public String id() {
        return id;
    }

    public void id(String id) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public String version() {
        return version;
    }

    public void version(String version) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public Container defined_in() {
        return ContainerHelper.narrow(defined_in.getReference());
    }

    public String absolute_name() {
        return absolute_name;
    }

    public Repository containing_repository() {
        return RepositoryHelper.narrow(repository.getReference());
    }

    public abstract Description describe();

    public void move(Container new_container,
                     String new_name, String new_version) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    /**
     * The global repository ID of this object.
     */
    protected String id;

    /**
     * The name of this object within its container.
     */
    protected String name;

    /**
     * The version of this object. Defaults to 1.0.
     */
    protected String version = "1.0";

    /**
     * The container this is defined in.
     * This may not be the same as the container this is contained in.
     */
    protected LocalContainer defined_in;

    /**
     * The absolute name of this object.
     */
    protected String absolute_name;


    /**
     * Return the POA object ID of this IR object.
     * Contained objects use the UTF-8 encoding of their id, prefixed by
     * "repository_name:".
     */
    protected byte[] getObjectId() {
        return (getRepository().getObjectIdPrefix() + id).getBytes(StandardCharsets.UTF_8);
    }
}
