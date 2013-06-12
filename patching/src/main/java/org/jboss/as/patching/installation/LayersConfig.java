package org.jboss.as.patching.installation;

import static org.jboss.as.patching.Constants.DEFAULT_ADD_ONS_PATH;
import static org.jboss.as.patching.Constants.DEFAULT_LAYERS_PATH;
import static org.jboss.as.patching.Constants.LAYERS_CONF;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.runner.PatchUtils;

/**
 * @author Brian Stansberry
 */
public class LayersConfig {

    private final boolean configured;
    private final String layersPath;
    private final String addOnsPath;
    private final List<String> layers;

    LayersConfig() {
        configured = false;
        layersPath = DEFAULT_LAYERS_PATH;
        addOnsPath = DEFAULT_ADD_ONS_PATH;
        layers = Collections.singletonList(Constants.BASE);
    }

    LayersConfig(Properties properties) {
        configured = true;
        layersPath = properties.getProperty("layers.path", DEFAULT_LAYERS_PATH);
        addOnsPath = properties.getProperty("add-ons.path", DEFAULT_ADD_ONS_PATH);
        final boolean excludeBase = Boolean.valueOf(properties.getProperty(Constants.EXCLUDE_LAYER_BASE, "false"));
        String layersProp = (String) properties.get(Constants.LAYERS);
        if (layersProp == null || (layersProp = layersProp.trim()).length() == 0) {
            if (excludeBase) {
                layers = Collections.emptyList();
            } else {
                layers = Collections.singletonList(Constants.BASE);
            }
        } else {
            final String[] layerNames = layersProp.split(",");
            final List<String> layers = new ArrayList<String>();
            boolean hasBase = false;
            for (String layerName : layerNames) {
                if (Constants.BASE.equals(layerName)) {
                    hasBase = true;
                }
                layers.add(layerName);
            }
            if (!hasBase && !excludeBase) {
                layers.add(Constants.BASE);
            }
            this.layers = Collections.unmodifiableList(layers);
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getLayersPath() {
        return layersPath;
    }

    public String getAddOnsPath() {
        return addOnsPath;
    }

    public List<String> getLayers() {
        return layers;
    }

    /**
     * Process the layers.conf file.
     *
     * @param repoRoot the root
     * @return the layers conf
     * @throws java.io.IOException
     */
    public static LayersConfig getLayersConfig(final File repoRoot) throws IOException {
        final File layersList = new File(repoRoot, LAYERS_CONF);
        if (!layersList.exists()) {
            return new LayersConfig();
        }
        final Properties properties = PatchUtils.loadProperties(layersList);
        return new LayersConfig(properties);
    }

}
