/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import jakarta.resource.cci.ResourceAdapterMetaData;

/**
 * MultipleRaMetaData
 *
 * @version $Revision: $
 */
public class MultipleRaMetaData implements ResourceAdapterMetaData {
    /**
     * Default constructor
     */
    public MultipleRaMetaData() {

    }

    /**
     * Gets the version of the resource adapter.
     *
     * @return String representing version of the resource adapter
     */
    @Override
    public String getAdapterVersion() {
        return null; //TODO
    }

    /**
     * Gets the name of the vendor that has provided the resource adapter.
     *
     * @return String representing name of the vendor
     */
    @Override
    public String getAdapterVendorName() {
        return null; //TODO
    }

    /**
     * Gets a tool displayable name of the resource adapter.
     *
     * @return String representing the name of the resource adapter
     */
    @Override
    public String getAdapterName() {
        return null; //TODO
    }

    /**
     * Gets a tool displayable short desription of the resource adapter.
     *
     * @return String describing the resource adapter
     */
    @Override
    public String getAdapterShortDescription() {
        return null; //TODO
    }

    /**
     * Returns a string representation of the version
     *
     * @return String representing the supported version of the connector architecture
     */
    @Override
    public String getSpecVersion() {
        return null; //TODO
    }

    /**
     * Returns an array of fully-qualified names of InteractionSpec
     *
     * @return Array of fully-qualified class names of InteractionSpec classes
     */
    @Override
    public String[] getInteractionSpecsSupported() {
        return null; //TODO
    }

    /**
     * Returns true if the implementation class for the Interaction
     *
     * @return boolean Depending on method support
     */
    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return false; //TODO
    }

    /**
     * Returns true if the implementation class for the Interaction
     *
     * @return boolean Depending on method support
     */
    @Override
    public boolean supportsExecuteWithInputRecordOnly() {
        return false; //TODO
    }

    /**
     * Returns true if the resource adapter implements the LocalTransaction
     *
     * @return true If resource adapter supports resource manager local transaction demarcation
     */
    @Override
    public boolean supportsLocalTransactionDemarcation() {
        return false; //TODO
    }


}
