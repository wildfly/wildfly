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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ListEditor extends JPanel {

    private ModelNode value;
    private JLabel strValue = new JLabel();
    private JButton editButton = new JButton("Edit");

    private Dialog parent;

    public ListEditor(Dialog parent) {
        this.parent = parent;
        value = new ModelNode();
        setValue(value);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(strValue);

        add(Box.createHorizontalStrut(5));
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Editor editor = new Editor(ListEditor.this.parent);
                editor.setVisible(true);
            }
        });
        add(editButton);

        add(Box.createHorizontalStrut(5));
        JButton undefineButton = new JButton("Undefine");
        undefineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ListEditor.this.setValue(new ModelNode());
            }
        });
        add(undefineButton);
    }

    public ModelNode getValue() {
        return this.value;
    }

    public void setValue(ModelNode value) {
        this.value = value;
        String labelText = value.asString();
        if (labelText.length() > 25) {
            labelText = labelText.substring(0, 25) + " ...]";
        }
        strValue.setText(labelText);
    }

    private class Editor extends JDialog {

        private List<JTextField> parsedValues = new ArrayList<JTextField>();

        public Editor(Dialog parentDialog) {
            super(parentDialog, "Edit List", true);
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout(10, 10));

            contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
            pack();
            setResizable(false);

            parseValues();

        }

        private void parseValues() {
            ModelNode list = ListEditor.this.getValue();
            if (!list.isDefined()) return;
            for (ModelNode item : list.asList()) {
                parsedValues.add(new JTextField(item.asString(), 15));
            }
        }

        private JScrollPane makeCenterPanel() {
            JPanel mainPanel = new JPanel(new BorderLayout());

            return new JScrollPane(mainPanel);
        }

        private JPanel makeButtonPanel() {
            JPanel buttonPanel = new JPanel();

            JButton ok = new JButton("OK");
            ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    ModelNode node = new ModelNode();
                    node.add("foo");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    node.add("barfasdfasdf");
                    ListEditor.this.setValue(node);
                    Editor.this.dispose();
                }
            });

            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Editor.this.dispose();
                }
            });

            buttonPanel.add(ok);
            buttonPanel.add(cancel);
            return buttonPanel;
        }


    }
}
