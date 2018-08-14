/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.hibernate.test;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.ProcedureParameterNamedBinder;

public class ProcedureParameterNamedBinderType implements ProcedureParameterNamedBinder {

    @Override
    public boolean canDoSetting() {
        return false;
    }

    @Override
    public void nullSafeSet(CallableStatement statement, Object value, String name, SessionImplementor session)
            throws SQLException {
    }

}
