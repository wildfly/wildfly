/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.wildfly.extension.picketlink.idm.model.parser.IDMSubsystemReader_1_0;
import org.wildfly.extension.picketlink.idm.model.parser.IDMSubsystemReader_2_0;
import org.wildfly.extension.picketlink.idm.model.parser.IDMSubsystemWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public enum Namespace {

    PICKETLINK_IDENTITY_MANAGEMENT_1_0(1, 0, 0, "1.0", new IDMSubsystemReader_1_0(), new IDMSubsystemWriter()),
    PICKETLINK_IDENTITY_MANAGEMENT_1_1(1, 1, 0, "1.1", new IDMSubsystemReader_2_0(), new IDMSubsystemWriter()),
    PICKETLINK_IDENTITY_MANAGEMENT_2_0(2, 0, 0, "2.0", new IDMSubsystemReader_2_0(), new IDMSubsystemWriter()),
    PICKETLINK_IDENTITY_MANAGEMENT_3_0(2, 0, 0, "2.0", new IDMSubsystemReader_2_0(), new IDMSubsystemWriter());

    public static final Namespace CURRENT = PICKETLINK_IDENTITY_MANAGEMENT_3_0;
    public static final String BASE_URN = "urn:jboss:domain:picketlink-identity-management:";

    private static final Map<String, Namespace> namespaces;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();

        for (Namespace namespace : values()) {
            final String name = namespace.getUri();
            if (name != null) {
                map.put(name, namespace);
            }
        }

        namespaces = map;
    }

    private final int major;
    private final int minor;
    private final int patch;
    private final String urnSuffix;
    private final XMLElementReader<List<ModelNode>> reader;
    private final XMLElementWriter<SubsystemMarshallingContext> writer;

    Namespace(int major, int minor, int patch, String urnSuffix, XMLElementReader<List<ModelNode>> reader,
            XMLElementWriter<SubsystemMarshallingContext> writer) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.urnSuffix = urnSuffix;
        this.reader = reader;
        this.writer = writer;
    }

    /**
     * Converts the specified uri to a {@link org.wildfly.extension.picketlink.idm.Namespace}.
     *
     * @param uri a namespace uri
     *
     * @return the matching namespace enum.
     */
    public static Namespace forUri(String uri) {
        return namespaces.get(uri) == null ? null : namespaces.get(uri);
    }

    /**
     * @return the major
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * @return the minor
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     *
     * @return the patch
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUri() {
        return BASE_URN + this.urnSuffix;
    }

    /**
     * Returns a xml reader for a specific namespace version.
     *
     * @return
     */
    public XMLElementReader<List<ModelNode>> getXMLReader() {
        return this.reader;
    }

    /**
     * Returns a xml writer for a specific namespace version.
     *
     * @return
     */
    public XMLElementWriter<SubsystemMarshallingContext> getXMLWriter() {
        return this.writer;
    }

    public ModelVersion getModelVersion() {
        if (this.patch > 0) {
            return ModelVersion.create(getMajor(), getMinor(), getPatch());
        }

        return ModelVersion.create(getMajor(), getMinor());
    }

}
