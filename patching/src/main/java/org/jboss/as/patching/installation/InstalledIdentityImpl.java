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

package org.jboss.as.patching.installation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
public class InstalledIdentityImpl extends InstalledIdentity {

    private final Identity identity;
    private final InstalledImage installedImage;
    private final List<String> allPatches;
    private final Map<String, Layer> layers = new LinkedHashMap<String, Layer>();
    private final Map<String, AddOn> addOns = new LinkedHashMap<String, AddOn>();

    protected InstalledIdentityImpl(final Identity identity, final List<String> allPatches, final InstalledImage installedImage) {
        this.identity = identity;
        this.installedImage = installedImage;
        this.allPatches = Collections.unmodifiableList(allPatches);
    }

    @Override
    public List<String> getAllInstalledPatches() {
        return allPatches;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public List<Layer> getLayers() {
        final List<Layer> layers = new ArrayList<Layer>(this.layers.values());
        return Collections.unmodifiableList(layers);
    }

    @Override
    public List<String> getLayerNames() {
        final List<String> layerNames = new ArrayList<String>(layers.keySet());
        return Collections.unmodifiableList(layerNames);
    }

    @Override
    public Layer getLayer(String layerName) {
        return layers.get(layerName);
    }

    @Override
    public Collection<String> getAddOnNames() {
        return Collections.unmodifiableCollection(this.addOns.keySet());
    }

    @Override
    public AddOn getAddOn(String addOnName) {
        return addOns.get(addOnName);
    }

    @Override
    public Collection<AddOn> getAddOns() {
        return Collections.unmodifiableCollection(this.addOns.values());
    }

    @Override
    public InstalledImage getInstalledImage() {
        return installedImage;
    }

    protected Layer putLayer(final String name, final Layer layer) {
        return layers.put(name, layer);
    }

    protected AddOn putAddOn(final String name, final AddOn addOn) {
        return addOns.put(name, addOn);
    }

}
