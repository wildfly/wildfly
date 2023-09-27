/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.jaspi;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.auth.jaspi.Flag;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * A {@link ConfigurableElement} to add a jaspi-configuration resource.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class JaspiConfiguration extends AbstractConfigurableElement {

    /*
     * Reduced visibility until we decide to make this accessible for use elsewhere which would also mean moving this class into
     * common.
     */

    private final PathAddress address;
    private final String layer;
    private final String applicationContext;
    private final String description;
    private final List<ModuleDefinition> modules;

    private JaspiConfiguration(Builder builder) {
        super(builder);
        this.address = PathAddress.pathAddress().append("subsystem", "elytron").append("jaspi-configuration", name);
        this.layer = builder.layer;
        this.applicationContext = builder.applicationContext;
        this.description = builder.description;
        this.modules = builder.modules;
    }

    @Override
   public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
       ModelNode add = Util.createAddOperation(address);
       setIfNotNull(add, "layer", layer);
       setIfNotNull(add, "application-context", applicationContext);
       setIfNotNull(add, "description", description);
       ModelNode modules = add.get("server-auth-modules");
       for (ModuleDefinition moduleDefinition : this.modules) {
           ModelNode module = new ModelNode();
           setIfNotNull(module, "class-name", moduleDefinition.className);
           setIfNotNull(module, "module", moduleDefinition.module);
           setIfNotNull(module, "flag", moduleDefinition.flag.toString());
           if (moduleDefinition.options != null) {
               ModelNode options = module.get("options");
               for (Entry<String, String> entry : moduleDefinition.options.entrySet()) {
                   options.get(entry.getKey()).set(entry.getValue());
               }
           }

           modules.add(module);
       }

       Utils.applyUpdate(add, client);
   }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(address), client);
    }

    /**
     * Creates a new {@link Builder} to configure a jaspi-configuration resource.
     *
     * @return a new {@link Builder} to configure a jaspi-configuration resource.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build an jaspi-configuration resource in the Elytron subsystem
     */
   public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

       private String layer;
       private String applicationContext;
       private String description;
       private List<ModuleDefinition> modules = new ArrayList<>();

       /**
        * Set the layer to be specified in this jaspi-configuration.
        *
        * @param layer the layer to be specified in this jaspi.configuration.
        * @return this {@link Builder} to allow method chaining.
        */
       public Builder withLayer(final String layer) {
           this.layer = layer;

           return this;
       }

       /**
        * Set the application-context to be specified in this jaspi-configuration.
        *
        * @param applicationContext the application-context to be specified in this jaspi-configuration.
        * @return this {@link Builder} to allow method chaining.
        */
       public Builder withApplicationContext(final String applicationContext) {
           this.applicationContext = applicationContext;

           return this;
       }

       /**
        * Set the description to be associated with this jaspi-configuration.
        *
        * @param description the description to be associated with this jaspi-configuration.
        * @return this {@link Builder} to allow method chaining.
        */
       public Builder withDescription(final String description) {
           this.description = description;

           return this;
       }

       /**
        * Add a server-auth-module definition for this jaspi-configuration resource.
        *
        * @param className the fully qualified class name of the ServerAuthModule
        * @param module the name of the module containing the ServerAuthModule or {@code null}
        * @param flag  the control flag for the ServerAuthModule or {@code null}
        * @param options configuration options to be passed to the ServerAuthModule} or {@code null}
        * @return this {@link Builder} to allow method chaining.
        */
       public Builder withServerAuthModule(final String className, final String module, final Flag flag, final Map<String, String> options) {
           modules.add(new ModuleDefinition(className, module, flag, options));

           return this;
       }

       /**
        * Build an instance of {@link JaspiConfiguration}
        *
        * @return an instance of {@link JaspiConfiguration}
        */
       public JaspiConfiguration build() {
           return new JaspiConfiguration(this);
       }

        @Override
        protected Builder self() {
            return this;
        }

   }

   static class ModuleDefinition {

       final String className;
       final String module;
       final Flag flag;
       final Map<String, String> options;

       ModuleDefinition(final String className, final String module, final Flag flag, final Map<String, String> options) {
           this.className = className;
           this.module = module;
           this.flag = flag;
           this.options = options;
       }

   }
}
