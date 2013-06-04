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

package org.jboss.as.patching.metadata;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.impl.IdentityImpl;
import org.jboss.as.patching.metadata.impl.UpgradeCallback;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchBuilder implements UpgradeCallback, Builder {

    private String patchId;
    private String description;
    private String resultingVersion;
    private PatchType patchType;
    private Identity identity;

    private final List<ContentModification> modifications = new ArrayList<ContentModification>();
    private final List<PatchElement> elements = new ArrayList<PatchElement>();

    public static PatchBuilder create() {
        return new PatchBuilder();
    }

    private PatchBuilder() {
    }

    public PatchBuilder setPatchId(String patchId) {
        this.patchId = patchId;
        return this;
    }

    public PatchBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public PatchBuilder setUpgrade(String toVersion) {
        this.patchType = PatchType.CUMULATIVE;
        this.resultingVersion = toVersion;
        return this;
    }

    @Override
    public PatchBuilder setNoUpgrade() {
        this.patchType = PatchType.ONE_OFF;
        return this;
    }

    public PatchBuilder setIdentity(Identity identity) {
        this.identity = identity;
        return this;
    }

    @Deprecated
    public PatchBuilder setCumulativeType(String appliesToVersion, String resultingVersion) {
        this.identity = new IdentityImpl("", appliesToVersion);
        setUpgrade(resultingVersion);
        return this;
    }

    @Deprecated
    public PatchBuilder setOneOffType(List<String> appliesTo) {
        assert appliesTo.size() == 1; // TODO update
        this.identity = new IdentityImpl("", appliesTo.get(0));
        setNoUpgrade();
        return this;
    }

    public PatchBuilder setOneOffType(String... appliesTo) {
        return setOneOffType(Arrays.asList(appliesTo));
    }

    public PatchBuilder addContentModification(ContentModification modification) {
        this.modifications.add(modification);
        return this;
    }

    public PatchBuilder addElement(PatchElement element) {
        this.elements.add(element);
        return this;
    }

    public List<ContentModification> getModifications() {
        return modifications;
    }

    @Override
    public Patch build() {
        assert notNull(identity);
        assert notNull(patchId);
        assert notNull(patchType);
        return new Patch() {

            @Override
            public String getResultingVersion() {
                return resultingVersion;
            }

            @Override
            public PatchType getPatchType() {
                return patchType;
            }

            @Override
            public String getPatchId() {
                return patchId;
            }

            @Override
            public List<ContentModification> getModifications() {
                return unmodifiableList(modifications);
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public List<String> getAppliesTo() {
                return Collections.singletonList(identity.getVersion());
            }

            @Override
            public Identity getIdentity() {
                return identity;
            }

            @Override
            public List<org.jboss.as.patching.metadata.PatchElement> getElements() {
                return elements;
            }
        };
    }

    static boolean notNull(Object o) {
        return o != null;
    }

}
