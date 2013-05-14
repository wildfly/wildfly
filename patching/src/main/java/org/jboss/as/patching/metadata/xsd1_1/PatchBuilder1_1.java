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

package org.jboss.as.patching.metadata.xsd1_1;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.patching.metadata.Builder;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.xsd1_1.impl.UpgradeCallback;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchBuilder1_1 implements UpgradeCallback, Builder {

    private String patchId;
    private String description;
    private String resultingVersion;
    private PatchType patchType;
    private Identity identity;
    private final List<PatchElement> elements = new ArrayList<PatchElement>();

    public static PatchBuilder1_1 create() {
        return new PatchBuilder1_1();
    }

    private PatchBuilder1_1() {
    }

    public PatchBuilder1_1 setPatchId(String patchId) {
        this.patchId = patchId;
        return this;
    }

    public PatchBuilder1_1 setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public PatchBuilder1_1 setUpgrade(String toVersion) {
        this.patchType = PatchType.CUMULATIVE;
        this.resultingVersion = toVersion;
        return this;
    }

    @Override
    public PatchBuilder1_1 setNoUpgrade() {
        this.patchType = PatchType.ONE_OFF;
        return this;
    }

    public PatchBuilder1_1 setIdentity(Identity identity) {
        this.identity = identity;
        return this;
    }

    public PatchBuilder1_1 addElement(PatchElement element) {
        this.elements.add(element);
        return this;
    }

    @Override
    public Patch1_1 build() {
        return new Patch1_1() {

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
            public List<PatchElement> getElements() {
                return unmodifiableList(elements);
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public Identity getIdentity() {
                return identity;
            }

            @Override
            public List<String> getAppliesTo() {
                // TODO this is adjusting to the previous api version
                return Collections.singletonList(identity.getVersion());
            }

            @Override
            public List<ContentModification> getModifications() {
                // TODO this is adjusting to the previous api version
                final List<ContentModification> modifications = new ArrayList<ContentModification>();
                for(PatchElement e : elements) {
                    for(ContentModification modification : e.getModifications()) {
                        modifications.add(modification);
                    }
                }
                return modifications;
            }
        };
    }
}
