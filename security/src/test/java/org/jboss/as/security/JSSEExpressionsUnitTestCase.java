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

package org.jboss.as.security;

import java.io.IOException;
import java.util.List;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Assert;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JSSEExpressionsUnitTestCase extends AbstractSubsystemBaseTest {


    public JSSEExpressionsUnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securityExpressions.xml");
    }

    @Override
    protected void validateModel(ModelNode model) {
        super.validateModel(model);
        ModelNode jsse = model.get("subsystem", "security", "security-domain", "other", "jsse", "classic");
        Assert.assertEquals(ModelType.OBJECT, jsse.getType());
        Assert.assertEquals(ModelType.EXPRESSION, jsse.get(Constants.CLIENT_ALIAS).getType());
        Assert.assertEquals(ModelType.EXPRESSION, jsse.get(Constants.SERVER_ALIAS).getType());
        Assert.assertEquals(ModelType.EXPRESSION, jsse.get(Constants.SERVICE_AUTH_TOKEN).getType());
        Assert.assertEquals(ModelType.EXPRESSION, jsse.get(Constants.CLIENT_AUTH).getType());
        Assert.assertEquals(ModelType.EXPRESSION, jsse.get(Constants.PROTOCOLS).getType());
        Assert.assertEquals(ModelType.EXPRESSION, jsse.get(Constants.CLIENT_ALIAS).getType());

        ModelNode keystore = jsse.get(Constants.KEYSTORE);
        Assert.assertEquals(ModelType.OBJECT, keystore.getType());
        Assert.assertEquals(ModelType.EXPRESSION, keystore.get(Constants.PASSWORD).getType());
        Assert.assertEquals(ModelType.EXPRESSION, keystore.get(Constants.TYPE).getType());
        Assert.assertEquals(ModelType.EXPRESSION, keystore.get(Constants.URL).getType());
        Assert.assertEquals(ModelType.EXPRESSION, keystore.get(Constants.PROVIDER).getType());
        Assert.assertEquals(ModelType.EXPRESSION, keystore.get(Constants.PROVIDER_ARGUMENT).getType());

        ModelNode truststore = jsse.get(Constants.TRUSTSTORE);
        Assert.assertEquals(ModelType.OBJECT, truststore.getType());
        Assert.assertEquals(ModelType.EXPRESSION, truststore.get(Constants.PASSWORD).getType());
        Assert.assertEquals(ModelType.EXPRESSION, truststore.get(Constants.TYPE).getType());
        Assert.assertEquals(ModelType.EXPRESSION, truststore.get(Constants.URL).getType());
        Assert.assertEquals(ModelType.EXPRESSION, truststore.get(Constants.PROVIDER).getType());
        Assert.assertEquals(ModelType.EXPRESSION, truststore.get(Constants.PROVIDER_ARGUMENT).getType());

        ModelNode keyManager = jsse.get(Constants.KEY_MANAGER);
        Assert.assertEquals(ModelType.OBJECT, keyManager.getType());
        Assert.assertEquals(ModelType.EXPRESSION, keyManager.get(Constants.ALGORITHM).getType());
        Assert.assertEquals(ModelType.EXPRESSION, keyManager.get(Constants.PROVIDER).getType());

        ModelNode trustManager = jsse.get(Constants.TRUST_MANAGER);
        Assert.assertEquals(ModelType.OBJECT, trustManager.getType());
        Assert.assertEquals(ModelType.EXPRESSION, trustManager.get(Constants.ALGORITHM).getType());
        Assert.assertEquals(ModelType.EXPRESSION, trustManager.get(Constants.PROVIDER).getType());

        // check if the module-option values have been created as expression nodes.
        ModelNode auth = model.get("subsystem", "security", "security-domain", "other", "authentication", "classic");
        Assert.assertEquals(ModelType.OBJECT, auth.getType());
        List<Property> loginModules = auth.get(Constants.LOGIN_MODULE).asPropertyList();
        //Assert.assertEquals(ModelType.LIST, loginModules.getType());
        ModelNode loginModule = loginModules.get(0).getValue();
        Assert.assertEquals(ModelType.OBJECT, auth.getType());
        for (Property prop : loginModule.get(Constants.MODULE_OPTIONS).asPropertyList()){
            Assert.assertEquals(ModelType.EXPRESSION, prop.getValue().getType());
        }


        ModelNode domain = model.get("subsystem", "security", "security-domain", "jboss-empty-jsse", "jsse", "classic");
        Assert.assertEquals(ModelType.OBJECT, domain.getType());
    }
}