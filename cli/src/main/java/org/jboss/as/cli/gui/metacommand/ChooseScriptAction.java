/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.cli.gui.metacommand;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.cli.gui.component.ScriptMenu;

/**
 * Action that allows the user to choose a script from the file system.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ChooseScriptAction extends ScriptAction {
    // make this static so that it always retains the last directory chosen
    private static JFileChooser fileChooser;

    public ChooseScriptAction(ScriptMenu menu, CliGuiContext cliGuiCtx) {
        super(menu, "Choose CLI Script", cliGuiCtx);
        putValue(SHORT_DESCRIPTION, "Choose a CLI script from the file system.");
    }

    public void actionPerformed(ActionEvent e) {
        // Do this here or it gets metal look and feel.  Not sure why.
        if (fileChooser == null) {
            fileChooser = new JFileChooser(new File("."));
        }

        int returnVal = fileChooser.showOpenDialog(cliGuiCtx.getMainPanel());
        if (returnVal != JFileChooser.APPROVE_OPTION) return;

        runScript(fileChooser.getSelectedFile());
    }

}
