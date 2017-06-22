package org.jboss.as.test.integration.deployment.classloading.jaxp;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * A simple builder of {@code jboss-deployment-structure.xml} files. Compared to
 * {@link CoreUtils#getJBossDeploymentStructure(String...)} this one supports the {@code services} attribute on
 * {@code module} elements.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class JBossDeploymentStructure {
    /**
     * A dependency item.
     */
    static class Module {
        private final String name;
        private final JBossDeploymentStructure.Services services;

        public Module(String name, JBossDeploymentStructure.Services services) {
            super();
            this.name = name;
            this.services = services;
        }

        public String getName() {
            return name;
        }

        public JBossDeploymentStructure.Services getServices() {
            return services;
        }
    }

    /**
     * An enum of valid values of the {@code services} attribute under the {@code module} element.
     */
    public enum Services {
        export("export"), import_("import"), none("none");
        private final String value;

        private Services(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private final List<JBossDeploymentStructure.Module> dependencies = new ArrayList<>();

    /**
     * @return the current {@link JBossDeploymentStructure} as an {@link Asset}
     */
    public Asset asAsset() {
        return new StringAsset(asString());
    }

    /**
     * @return the current {@link JBossDeploymentStructure} as a {@link String}
     */
    public String asString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-deployment-structure><deployment><dependencies>");
        for (JBossDeploymentStructure.Module dep : dependencies) {
            sb.append("\n\t<module name=\"").append(dep.getName()).append("\"");
            switch (dep.getServices()) {
            case none:
                /* do not render the default value explictily */
                break;
            default:
                sb.append(" services=\"").append(dep.getServices().toString()).append("\"");
                break;
            }
            sb.append("/>");
        }
        sb.append("\n</dependencies></deployment></jboss-deployment-structure>");
        return sb.toString();
    }

    /**
     * Adds a new module under {@code jboss-deployment-structure/deployment/dependencies}
     *
     * @param name
     *            the name of the module to add
     * @return this {@link JBossDeploymentStructure}
     */
    public JBossDeploymentStructure dependency(String name) {
        return dependency(name, Services.none);
    }

    /**
     * Adds a new module under {@code jboss-deployment-structure/deployment/dependencies}
     *
     * @param name
     *            the name of the module to add
     * @param services
     *            the value of the {@code services} attribute
     * @return this {@link JBossDeploymentStructure}
     */
    public JBossDeploymentStructure dependency(String name, JBossDeploymentStructure.Services services) {
        dependencies.add(new Module(name, services));
        return this;
    }
}