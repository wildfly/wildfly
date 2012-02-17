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
package org.jboss.as.cli.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * JDialog that allows the user to specify the params for an operation.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class OperationDialog extends JDialog {

    private ManagementModelNode node;
    private String opName;
    private SortedSet<RequestProp> props;

    public OperationDialog(ManagementModelNode node, String opName, String strDescription, ModelNode requestProperties) {
        super(GuiMain.getFrame(), opName, true);
        this.node = node;
        this.opName = opName;
        setProps(requestProperties);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        JLabel opNameLabel = new JLabel("Params for " + opName + ":");
        opNameLabel.setToolTipText(strDescription);
        contentPane.add(opNameLabel, BorderLayout.NORTH);

        contentPane.add(makeInputPanel(), BorderLayout.CENTER);

        contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(false);
    }

    @Override
    public void setVisible(boolean isVisible) {
        if (node.isLeaf()) {
            // "rightSide" field should have focus for write-attribute dialog
            // where "leftSide" field is already populated
            for (RequestProp prop : props) {
                if (prop.getName().equals("value")) {
                    prop.getValueComponent().requestFocus();
                }
            }
        }

        super.setVisible(isVisible);
    }

    private void setProps(ModelNode requestProperties) {
        props = new TreeSet<RequestProp>();
        if (opName.equals("add")) {
            UserObject usrObj = (UserObject)node.getUserObject();
            props.add(new RequestProp("/" + usrObj.getName() + "=<name>/", "Resource name for the new " + usrObj.getName(), true));
        }

        for (Property prop : requestProperties.asPropertyList()) {
            props.add(new RequestProp(prop.getName(), prop.getValue()));
        }
    }

    private JPanel makeInputPanel() {
        boolean hasRequiredFields = false;
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbConst = new GridBagConstraints();
        gbConst.anchor = GridBagConstraints.WEST;
        gbConst.insets = new Insets(5,5,5,5);

        for (RequestProp prop : props) {
            JLabel label = prop.getLabel();
            gbConst.gridwidth = 1;
            inputPanel.add(label, gbConst);

            inputPanel.add(Box.createHorizontalStrut(5));

            JComponent comp = prop.getValueComponent();
            gbConst.gridwidth = GridBagConstraints.REMAINDER;
            inputPanel.add(comp, gbConst);

            if (prop.isRequired) hasRequiredFields = true;
        }

        if (hasRequiredFields) {
            inputPanel.add(new JLabel(" * = Required Field"));
        }

        return inputPanel;
    }

    private JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JButton ok = new JButton("OK");
        ok.addActionListener(new SetOperationActionListener());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                OperationDialog.this.dispose();
            }
        });

        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        return buttonPanel;
    }

    /**
     * Set the command line with address + operation + params.
     */
    private class SetOperationActionListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {

            String addressPath = OperationDialog.this.node.addressPath();
            if (OperationDialog.this.opName.equals("add")) {
                UserObject usrObj = (UserObject)OperationDialog.this.node.getUserObject();
                ManagementModelNode parent = (ManagementModelNode)OperationDialog.this.node.getParent();
                RequestProp resourceProp = OperationDialog.this.props.first();
                String value = resourceProp.getValueAsString();
                value = ManagementModelNode.escapeAddressElement(value);
                addressPath = parent.addressPath() + usrObj.getName() + "=" + value + "/";
                OperationDialog.this.props.remove(resourceProp);
            }

            StringBuilder command = new StringBuilder();
            command.append(addressPath);
            command.append(":");
            command.append(OperationDialog.this.opName);
            addRequestProps(command, OperationDialog.this.props);

            GuiMain.getCommandText().setText(command.toString());
            OperationDialog.this.dispose();
            GuiMain.getCommandText().requestFocus();
        }

        private void addRequestProps(StringBuilder command, SortedSet<RequestProp> reqProps) {
            boolean addedProps = false;
            command.append("(");
            for (RequestProp prop : reqProps) {
                String value = prop.getValueAsString();

                if (value == null) continue;
                if (value.equals("")) continue;

                addedProps = true;
                command.append(prop.getName());
                command.append("=");
                command.append(value);

                command.append(",");
            }

            if (addedProps) {
                // replace training comma with close paren
                command.replace(command.length()-1, command.length(), ")");
            } else {
                // remove opening paren
                command.deleteCharAt(command.length() - 1);
            }
        }

    }

    /**
     * Request property class.  This class contains all known information about each operation attribute.
     *
     * It is also responsible for building the input component for the attribute.
     */
    private class RequestProp implements Comparable {
        private final String name;
        private ModelNode props;
        private ModelType type;
        private String description;
        private boolean isRequired = false;
        private boolean nillable = false;
        private ModelNode defaultValue = null;

        private JLabel label;
        private JComponent valueComponent;

        private boolean isResourceName = false;

        /**
         * Constructor used for resource name property.
         * @param name Property name
         * @param description Description for tool tip text.
         * @param required Is this a isRequired property?
         */
        public RequestProp(String name, String description, boolean required) {
            this.name = name;
            this.props = new ModelNode();
            this.description = description;
            this.type = ModelType.STRING;
            this.isRequired = required;
            this.isResourceName = true;
            setInputComponent();
        }

        public RequestProp(String name, ModelNode props) {
            this.name = name;
            this.props = props;
            this.type = props.get("type").asType();

            if (props.get("description").isDefined()) {
                this.description = props.get("description").asString();
            }

            if (props.get("required").isDefined()) {
                this.isRequired = props.get("required").asBoolean();
            }

            if (props.get("nillable").isDefined()) {
                this.nillable = props.get("nillable").asBoolean();
            }

            if (props.get("default").isDefined()) {
                this.defaultValue = props.get("default");
            }

            setInputComponent();
            setWriteAttributeValues();
        }

        public String getName() {
            return name;
        }

        public JComponent getValueComponent() {
            return valueComponent;
        }

        public JLabel getLabel() {
            return this.label;
        }

        public String getValueAsString() {
            if (valueComponent instanceof JTextComponent) {
                return ((JTextComponent)valueComponent).getText();
            }

            if (valueComponent instanceof AbstractButton) {
                return Boolean.toString(((AbstractButton)valueComponent).isSelected());
            }

            if (valueComponent instanceof JComboBox) {
                return ((JComboBox)valueComponent).getSelectedItem().toString();
            }

            return null;
        }

        private void setInputComponent() {
            if (type == ModelType.BOOLEAN) {
                if (defaultValue == null) {
                    this.valueComponent = new JCheckBox(makeLabelString(false));
                } else {
                    this.valueComponent = new JCheckBox(makeLabelString(false), defaultValue.asBoolean());
                }
                this.valueComponent.setToolTipText(description);
                this.label = new JLabel();
            } else if (props.get("allowed").isDefined()) {
                JComboBox comboBox = makeJComboBox(props.get("allowed").asList());
                if (defaultValue != null) comboBox.setSelectedItem(defaultValue.asString());
                this.valueComponent = comboBox;
                this.label = makeLabel();
            } else {
                JTextField textField = new JTextField(30);
                if (defaultValue != null) textField.setText(defaultValue.asString());
                this.valueComponent = textField;
                this.label = makeLabel();
            }

        }

        private String makeLabelString(boolean addColon) {
            String labelString = name;
            if (addColon) labelString += ":";
            if (isRequired) labelString += " *";
            return labelString;
        }

        private JLabel makeLabel() {
            JLabel label = new JLabel(makeLabelString(true));
            label.setToolTipText(description);
            return label;
        }

        private JComboBox makeJComboBox(List<ModelNode> values) {
            Vector<String> valueVector = new Vector<String>(values.size());
            if (!isRequired) {
                valueVector.add("");
            }

            for (ModelNode node : values) {
                valueVector.add(node.asString());
            }
            return new JComboBox(valueVector);
        }

        // fill in form fields for write-attribute when an attribute node is selected.
        private void setWriteAttributeValues() {
            if (!OperationDialog.this.node.isLeaf()) return;
            if (!OperationDialog.this.opName.equals("write-attribute")) return;

            UserObject usrObj = (UserObject)OperationDialog.this.node.getUserObject();

            if (this.name.equals("name")) {
                ((JTextField)valueComponent).setText(usrObj.getName());
                return;
            }

            if (usrObj.getValue().equals("undefined")) return;

            if (this.name.equals("value")) ((JTextField)valueComponent).setText(usrObj.getValue());
        }

        @Override
        public int compareTo(Object t) {
            if (this.equals(t)) return 0;
            if (this.isResourceName) return -1;
            RequestProp compareTo = (RequestProp)t;
            if (this.isRequired && compareTo.isRequired) return 1;
            if (this.isRequired) return -1;
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RequestProp)) return false;
            RequestProp compareTo = (RequestProp)obj;
            return this.name.equals(compareTo.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

    }

}
