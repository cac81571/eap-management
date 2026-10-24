package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.eap.management.config.EnvVar;

public class EnvTablePanel extends JPanel {
    private final EnvTableModel model = new EnvTableModel();
    private final JTable table = new JTable(model);
    private Runnable onChange = () -> {
    };

    public EnvTablePanel() {
        super(new BorderLayout(8, 8));
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(320);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JButton add = new JButton("追加");
        JButton remove = new JButton("削除");
        add.addActionListener(e -> {
            model.addRow();
            onChange.run();
        });
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                model.removeRow(row);
                onChange.run();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(add);
        buttons.add(remove);

        add(buttons, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setVars(List<EnvVar> vars) {
        model.setVars(vars);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange == null ? () -> {
        } : onChange;
        model.setOnChange(this.onChange);
    }

    private static final class EnvTableModel extends AbstractTableModel {
        private final String[] columns = {"有効", "名前", "値"};
        private List<EnvVar> vars = List.of();
        private Runnable onChange = () -> {
        };

        void setVars(List<EnvVar> vars) {
            this.vars = vars;
            fireTableDataChanged();
        }

        void setOnChange(Runnable onChange) {
            this.onChange = onChange;
        }

        void addRow() {
            vars.add(new EnvVar("KEY", ""));
            fireTableRowsInserted(vars.size() - 1, vars.size() - 1);
        }

        void removeRow(int row) {
            vars.remove(row);
            fireTableRowsDeleted(row, row);
        }

        @Override
        public int getRowCount() {
            return vars.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            EnvVar var = vars.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> var.isEnabled();
                case 1 -> var.getKey();
                case 2 -> var.getValue();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            EnvVar var = vars.get(rowIndex);
            switch (columnIndex) {
                case 0 -> var.setEnabled(Boolean.TRUE.equals(aValue));
                case 1 -> var.setKey(String.valueOf(aValue));
                case 2 -> var.setValue(aValue == null ? "" : String.valueOf(aValue));
                default -> {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
            onChange.run();
        }
    }
}
