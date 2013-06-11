package org.jboss.as.patching.installation;

import static org.jboss.as.patching.Constants.DEFAULT_ADD_ONS_PATH;
import static org.jboss.as.patching.Constants.DEFAULT_LAYERS_PATH;
import static org.jboss.as.patching.Constants.LAYERS_CONF;
import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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
        layers = Collections.singletonList("base");
    }

    LayersConfig(Properties properties) {
        configured = true;
        layersPath = properties.getProperty("layers.path", DEFAULT_LAYERS_PATH);
        addOnsPath = properties.getProperty("add-ons.path", DEFAULT_ADD_ONS_PATH);
        boolean excludeBase = Boolean.valueOf(properties.getProperty("exclude.base.layer", "false"));
        String layersProp = (String) properties.get("layers");
        if (layersProp == null || (layersProp = layersProp.trim()).length() == 0) {
            if (excludeBase) {
                layers = Collections.emptyList();
            } else {
                layers = Collections.singletonList("base");
            }
        } else {
            String[] layerNames = layersProp.split(",");
            final List<String> layers = new ArrayList<String>();
            boolean hasBase = false;
            for (String layerName : layerNames) {
                if ("base".equals(layerName)) {
                    hasBase = true;
                }
                layers.add(layerName);
            }
            if (!hasBase && !excludeBase) {
                layers.add("base");
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
        final Properties properties = loadProperties(layersList);
        return new LayersConfig(properties);
    }

    static Properties loadProperties(final File file) throws IOException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            final Properties props = new Properties();
            props.load(reader);
            return props;
        } finally {
            safeClose(reader);
        }
    }

}
