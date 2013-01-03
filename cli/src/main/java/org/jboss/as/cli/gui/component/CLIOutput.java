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
package org.jboss.as.cli.gui.component;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * This is the JTextPane used for the Output tab.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CLIOutput extends JTextPane {

    public CLIOutput() {
        super();
        super.setEditable(false);
    }

    @Override
    public void setEditable(boolean b) {
        return; // don't allow editing
    }

    public void postCommandWithResponse(String command, String response) {
        postAttributed(response + "\n\n", (AttributeSet)null);
        postBold(command + "\n");
    }

    public void post(String text) {
        postAttributed(text, (AttributeSet)null);
    }

    public int postAt(String text, int position) {
        return postAttributed(text, position, (AttributeSet)null);
    }

    public void postBold(String text) {
        postBoldAt(text, 0);
    }

    public int postBoldAt(String text, int position) {
        SimpleAttributeSet attribs = new SimpleAttributeSet();
        StyleConstants.setBold(attribs, true);
        return postAttributed(text, position, attribs);
    }

    public void postAttributed(String text, AttributeSet attribs) {
        postAttributed(text, 0, attribs);
    }

    public int postAttributed(String text, int position, AttributeSet attribs) {
        Document doc = getDocument();
        setCaretPosition(position);

        try {
            doc.insertString(position, text, attribs);
            repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return getCaretPosition();
    }
}
