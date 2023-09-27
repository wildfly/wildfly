/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.commands;

import org.jboss.as.jdr.util.FSTree;

public class TreeCommand extends JdrCommand {

    @Override
    public void execute() throws Exception {
        FSTree tree = new FSTree(this.env.getJbossHome());
        this.env.getZip().add(tree.toString(), "tree.txt");
    }
}
