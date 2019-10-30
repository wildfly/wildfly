/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask;

/**
 * This server setup task creates a krb5.conf file and generates KeyTab files for the HTTP server and users hnelson and jduke.
 *
 * @author Josef Cacek
 */
public class Krb5ConfServerSetupTask extends AbstractKrb5ConfServerSetupTask {

    public static final File HNELSON_KEYTAB_FILE = new File(WORK_DIR, "hnelson.keytab");
    public static final File JDUKE_KEYTAB_FILE = new File(WORK_DIR, "jduke.keytab");

    @Override
    protected List<UserForKeyTab> kerberosUsers() {
        List<UserForKeyTab> users = new ArrayList<UserForKeyTab>();
        users.add(new UserForKeyTab("hnelson@JBOSS.ORG", "secret", HNELSON_KEYTAB_FILE));
        users.add(new UserForKeyTab("jduke@JBOSS.ORG", "theduke", JDUKE_KEYTAB_FILE));
        return users;
    }

}
