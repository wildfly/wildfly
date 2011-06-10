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
package org.jboss.as.cli.handlers.jca;

import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.RequestParamArgWithValue;
import org.jboss.as.cli.impl.RequestParamArgWithoutValue;
import org.jboss.as.cli.impl.RequestParamPropertiesArg;
import org.jboss.as.cli.impl.RequiredRequestParamArg;


/**
 *
 * @author Alexey Loubyansky
 */
public class XADataSourceAddHandler extends BaseDataSourceAddHandler {

    public XADataSourceAddHandler() {
        super("xa-data-source-add", "xa-data-source");
    }

    @Override
    public RequestParamArgWithValue initRequiredArguments() {
        ArgumentWithoutValue lastRequired = super.initRequiredArguments();
        RequestParamArgWithValue xaDataSourceClass = new RequiredRequestParamArg("xa-data-source-class", this);
        xaDataSourceClass.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue xaProps = new RequestParamPropertiesArg("xa-data-source-properties", this);
        xaProps.addRequiredPreceding(xaDataSourceClass);

        return xaProps;
    }

    @Override
    public void initOptionalArguments(ArgumentWithoutValue lastRequired) {
        super.initOptionalArguments(lastRequired);

        RequestParamArgWithoutValue interliving = new RequestParamArgWithoutValue("interliving", this);
        interliving.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue noTxSeparatePool = new RequestParamArgWithoutValue("no-tx-separate-pool", this);
        noTxSeparatePool.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue padXid = new RequestParamArgWithoutValue("pad-xid", this);
        padXid.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue sameRMOverride = new RequestParamArgWithoutValue("same-rm-override", this);
        sameRMOverride.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue wrapXADataSource = new RequestParamArgWithoutValue("wrap-xa-datasource", this);
        wrapXADataSource.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue noRecovery = new RequestParamArgWithoutValue("no-recovery", this);
        noRecovery.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue recoveryUsername = new RequestParamArgWithValue("recovery-username", this);
        recoveryUsername.addRequiredPreceding(lastRequired);
        recoveryUsername.addCantAppearAfter(noRecovery);
        noRecovery.addCantAppearAfter(recoveryUsername);

        RequestParamArgWithValue recoveryPassword = new RequestParamArgWithValue("recovery-password", this);
        recoveryPassword.addRequiredPreceding(lastRequired);
        recoveryPassword.addCantAppearAfter(noRecovery);
        noRecovery.addCantAppearAfter(recoveryPassword);

        RequestParamArgWithValue recoverySecurityDomain = new RequestParamArgWithValue("recovery-security-domain", this);
        recoverySecurityDomain.addRequiredPreceding(lastRequired);
        recoverySecurityDomain.addCantAppearAfter(noRecovery);
        noRecovery.addCantAppearAfter(recoverySecurityDomain);

        RequestParamArgWithValue recoveryPluginClass = new RequestParamArgWithValue("recovery-plugin-class-name", this, "--recovery-plugin-class");
        recoveryPluginClass.addRequiredPreceding(lastRequired);
        recoveryPluginClass.addCantAppearAfter(noRecovery);
        noRecovery.addCantAppearAfter(recoveryPluginClass);

        RequestParamArgWithValue recoveryPluginProps = new RequestParamPropertiesArg("recovery-plugin-properties", this);
        recoveryPluginProps.addRequiredPreceding(lastRequired);
        recoveryPluginProps.addCantAppearAfter(noRecovery);
        noRecovery.addCantAppearAfter(recoveryPluginProps);

        RequestParamArgWithValue xaResourceTimeout =  new RequestParamArgWithValue("xa-resource-timeout", this);
        xaResourceTimeout.addRequiredPreceding(lastRequired);
    }
}
