/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CORE_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.LITE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_POOL_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;

/**
 * Static utilities containing subsystem resource and operation descriptions. Separated from the
 * {@link DescriptionProvider} implementations so the cost of loading this code doesn't
 * get incurred during boot time
 * <p/>
 * User: jpai
 */
public class EJB3SubsystemDescriptions {

    static final String RESOURCE_NAME = EJB3SubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final ModelNode getSubystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("ejb3"));
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(EJB3Extension.NAMESPACE_1_0);

        subsystem.get(ATTRIBUTES, LITE, TYPE).set(ModelType.BOOLEAN);
        subsystem.get(ATTRIBUTES, LITE, DESCRIPTION).set(bundle.getString("ejb3.lite"));
        subsystem.get(ATTRIBUTES, LITE, DEFAULT).set(false);
        subsystem.get(ATTRIBUTES, LITE, REQUIRED).set(false);

        subsystem.get(ATTRIBUTES, DEFAULT_MDB_INSTANCE_POOL, DESCRIPTION).set(bundle.getString("ejb3.default-mdb-instance-pool"));
        subsystem.get(ATTRIBUTES, DEFAULT_MDB_INSTANCE_POOL, TYPE).set(ModelType.STRING);
        subsystem.get(ATTRIBUTES, DEFAULT_MDB_INSTANCE_POOL, REQUIRED).set(false);

        subsystem.get(ATTRIBUTES, DEFAULT_SLSB_INSTANCE_POOL, DESCRIPTION).set(bundle.getString("ejb3.default-slsb-instance-pool"));
        subsystem.get(ATTRIBUTES, DEFAULT_SLSB_INSTANCE_POOL, TYPE).set(ModelType.STRING);
        subsystem.get(ATTRIBUTES, DEFAULT_SLSB_INSTANCE_POOL, REQUIRED).set(false);

        subsystem.get(ATTRIBUTES, DEFAULT_RESOURCE_ADAPTER_NAME, DESCRIPTION).set(bundle.getString("ejb3.default-resource-adapter-name"));
        subsystem.get(ATTRIBUTES, DEFAULT_RESOURCE_ADAPTER_NAME, TYPE).set(ModelType.STRING);
        subsystem.get(ATTRIBUTES, DEFAULT_RESOURCE_ADAPTER_NAME, REQUIRED).set(false);

        subsystem.get(OPERATIONS); // placeholder

        subsystem.get(CHILDREN, SERVICE, DESCRIPTION).set(bundle.getString("ejb3.service"));
        subsystem.get(CHILDREN, SERVICE, MIN_OCCURS).set(0);
        subsystem.get(CHILDREN, SERVICE, MODEL_DESCRIPTION);

        subsystem.get(CHILDREN, STRICT_MAX_BEAN_INSTANCE_POOL, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool"));
        subsystem.get(CHILDREN, STRICT_MAX_BEAN_INSTANCE_POOL, MIN_OCCURS).set(0);
        subsystem.get(CHILDREN, STRICT_MAX_BEAN_INSTANCE_POOL, MODEL_DESCRIPTION);

        return subsystem;
    }

    static final ModelNode getSubystemAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("ejb3.add"));

        op.get(REQUEST_PROPERTIES, LITE, TYPE).set(ModelType.BOOLEAN);
        op.get(REQUEST_PROPERTIES, LITE, DESCRIPTION).set(bundle.getString("ejb3.lite"));
        op.get(REQUEST_PROPERTIES, LITE, DEFAULT).set(false);
        op.get(REQUEST_PROPERTIES, LITE, REQUIRED).set(false);

        op.get(REQUEST_PROPERTIES, DEFAULT_MDB_INSTANCE_POOL, DESCRIPTION).set(bundle.getString("ejb3.default-mdb-instance-pool"));
        op.get(REQUEST_PROPERTIES, DEFAULT_MDB_INSTANCE_POOL, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, DEFAULT_MDB_INSTANCE_POOL, REQUIRED).set(false);

        op.get(REQUEST_PROPERTIES, DEFAULT_SLSB_INSTANCE_POOL, DESCRIPTION).set(bundle.getString("ejb3.default-slsb-instance-pool"));
        op.get(REQUEST_PROPERTIES, DEFAULT_SLSB_INSTANCE_POOL, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, DEFAULT_SLSB_INSTANCE_POOL, REQUIRED).set(false);

        op.get(REQUEST_PROPERTIES, DEFAULT_RESOURCE_ADAPTER_NAME, DESCRIPTION).set(bundle.getString("ejb3.default-resource-adapter-name"));
        op.get(REQUEST_PROPERTIES, DEFAULT_RESOURCE_ADAPTER_NAME, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, DEFAULT_RESOURCE_ADAPTER_NAME, REQUIRED).set(false);

        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    static ModelNode getTimerServiceDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode description = new ModelNode();
        description.get(DESCRIPTION).set(bundle.getString("ejb3.timerservice"));

        // setup the "max-pool-size" param description
        description.get(ATTRIBUTES, MAX_THREADS, DESCRIPTION).set(bundle.getString("ejb3.timerservice.maxThreads"));
        description.get(ATTRIBUTES, MAX_THREADS, TYPE).set(ModelType.INT);
        description.get(ATTRIBUTES, MAX_THREADS, REQUIRED).set(false);
        // This is wrong in a domain, as the number of available processors varies from host to host. Just explain the default in the description
//        description.get(ATTRIBUTES, MAX_THREADS, DEFAULT).set(Runtime.getRuntime().availableProcessors());

        description.get(ATTRIBUTES, CORE_THREADS, DESCRIPTION).set(bundle.getString("ejb3.timerservice.coreThreads"));
        description.get(ATTRIBUTES, CORE_THREADS, TYPE).set(ModelType.INT);
        description.get(ATTRIBUTES, CORE_THREADS, REQUIRED).set(false);
        description.get(ATTRIBUTES, CORE_THREADS, DEFAULT).set(0);
        description.get(ATTRIBUTES, CORE_THREADS, MIN_VALUE).set(0);

        description.get(ATTRIBUTES, PATH, DESCRIPTION).set(bundle.getString("ejb3.timerservice.path"));
        description.get(ATTRIBUTES, PATH, TYPE).set(ModelType.STRING);
        description.get(ATTRIBUTES, PATH, REQUIRED).set(false);

        description.get(ATTRIBUTES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("ejb3.timerservice.relativeTo"));
        description.get(ATTRIBUTES, RELATIVE_TO, TYPE).set(ModelType.STRING);
        description.get(ATTRIBUTES, RELATIVE_TO, REQUIRED).set(false);

        description.get(OPERATIONS); // placeholder

        description.get(CHILDREN).setEmptyObject();

        return description;
    }

    /**
     * Description provider for the timer-service add operation
     */
    static ModelNode getTimerServiceAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode description = new ModelNode();
        // set the description of this operation
        description.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("ejb3.timerservice.add"));

        // setup the "max-pool-size" param description
        description.get(REQUEST_PROPERTIES, MAX_THREADS, DESCRIPTION).set(bundle.getString("ejb3.timerservice.maxThreads"));
        description.get(REQUEST_PROPERTIES, MAX_THREADS, TYPE).set(ModelType.INT);
        description.get(REQUEST_PROPERTIES, MAX_THREADS, REQUIRED).set(false);
        description.get(REQUEST_PROPERTIES, MAX_THREADS, DEFAULT).set(Runtime.getRuntime().availableProcessors());

        description.get(REQUEST_PROPERTIES, CORE_THREADS, DESCRIPTION).set(bundle.getString("ejb3.timerservice.coreThreads"));
        description.get(REQUEST_PROPERTIES, CORE_THREADS, TYPE).set(ModelType.INT);
        description.get(REQUEST_PROPERTIES, CORE_THREADS, REQUIRED).set(false);
        description.get(REQUEST_PROPERTIES, CORE_THREADS, DEFAULT).set(0);

        description.get(REQUEST_PROPERTIES, PATH, DESCRIPTION).set(bundle.getString("ejb3.timerservice.path"));
        description.get(REQUEST_PROPERTIES, PATH, TYPE).set(ModelType.STRING);
        description.get(REQUEST_PROPERTIES, PATH, REQUIRED).set(false);

        description.get(REQUEST_PROPERTIES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("ejb3.timerservice.relativeTo"));
        description.get(REQUEST_PROPERTIES, RELATIVE_TO, TYPE).set(ModelType.STRING);
        description.get(REQUEST_PROPERTIES, RELATIVE_TO, REQUIRED).set(false);

        return description;
    }

    static ModelNode getTimerServiceRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode description = new ModelNode();
        // setup the description
        description.get(DESCRIPTION).set(bundle.getString("ejb3.timerservice.remove"));
        return description;
    }

    static ModelNode getStrictMaxBeanInstancePoolDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode description = new ModelNode();
        // setup the description for the strict-max-bean-instance-pool address
        description.get(DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool"));

        // setup the "max-pool-size" param description
        description.get(ATTRIBUTES, MAX_POOL_SIZE, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.max-pool-size"));
        description.get(ATTRIBUTES, MAX_POOL_SIZE, TYPE).set(ModelType.INT);
        description.get(ATTRIBUTES, MAX_POOL_SIZE, REQUIRED).set(false);
        description.get(ATTRIBUTES, MAX_POOL_SIZE, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_MAX_POOL_SIZE);

        // setup the "timeout" param description
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.instance-acquisition-timeout"));
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT, TYPE).set(ModelType.INT);
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT, REQUIRED).set(false);
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_TIMEOUT);

        // setup the "timeout-unit" param description
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.instance-acquisition-timeout-unit"));
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, TYPE).set(ModelType.STRING);
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, REQUIRED).set(false);
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_TIMEOUT_UNIT.name());
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.HOURS.name());
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.MINUTES.name());
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.SECONDS.name());
        description.get(ATTRIBUTES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.MILLISECONDS.name());

        description.get(OPERATIONS); // placeholder

        description.get(CHILDREN).setEmptyObject();

        return description;
    }

    static ModelNode getStrictMaxPoolAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode description = new ModelNode();
        // set the description of this operation
        description.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.add"));

        // setup the "max-pool-size" param description
        description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.max-pool-size"));
        description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, TYPE).set(ModelType.INT);
        description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, REQUIRED).set(false);
        description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_MAX_POOL_SIZE);

        // setup the "timeout" param description
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.instance-acquisition-timeout"));
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, TYPE).set(ModelType.INT);
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, REQUIRED).set(false);
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_TIMEOUT);

        // setup the "timeout-unit" param description
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.instance-acquisition-timeout-unit"));
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, TYPE).set(ModelType.STRING);
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, REQUIRED).set(false);
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_TIMEOUT_UNIT.name());
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.HOURS.name());
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.MINUTES.name());
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.SECONDS.name());
        description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.MILLISECONDS.name());

        return description;
    }

    static ModelNode getStrictMaxPoolRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode description = new ModelNode();
        // setup the description
        description.get(DESCRIPTION).set(bundle.getString("ejb3.strict-max-bean-instance-pool.remove"));

        return description;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
