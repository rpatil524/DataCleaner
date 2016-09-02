/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.windows;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.apache.metamodel.excel.ExcelConfiguration;
import org.apache.metamodel.schema.naming.CustomColumnNamingStrategy;
import org.apache.metamodel.util.FileResource;
import org.apache.metamodel.util.Resource;
import org.datacleaner.bootstrap.WindowContext;
import org.datacleaner.connection.ExcelDatastore;
import org.datacleaner.guice.Nullable;
import org.datacleaner.panels.DCPanel;
import org.datacleaner.user.MutableDatastoreCatalog;
import org.datacleaner.user.UserPreferences;
import org.datacleaner.util.ErrorUtils;
import org.datacleaner.util.FileFilters;
import org.datacleaner.util.IconUtils;
import org.datacleaner.util.ImmutableEntry;
import org.datacleaner.util.WidgetFactory;
import org.datacleaner.widgets.AbstractResourceTextField;
import org.datacleaner.widgets.ResourceTypePresenter;
import org.jdesktop.swingx.HorizontalLayout;

public final class ExcelDatastoreDialog extends AbstractFileBasedDatastoreDialog<ExcelDatastore> {

    private static final long serialVersionUID = 1L;

    private final JButton _addColumnNamesButton;
    private final DCPanel _addColumnNamesPanel;
    private List<String> _columnNames;
    
    private volatile boolean showPreview = true;

    @Inject
    protected ExcelDatastoreDialog(@Nullable ExcelDatastore originalDatastore,
            MutableDatastoreCatalog mutableDatastoreCatalog, WindowContext windowContext,
            UserPreferences userPreferences) {
        super(originalDatastore, mutableDatastoreCatalog, windowContext, userPreferences);

        _addColumnNamesButton = WidgetFactory.createDefaultButton("Add", IconUtils.ACTION_RENAME);

        if (originalDatastore != null) {
            _columnNames = originalDatastore.getCustomColumnNames();
            _addColumnNamesButton.setEnabled(true);
        } else {
            _columnNames = null;
            _addColumnNamesButton.setEnabled(false);
        }
        _addColumnNamesPanel = new DCPanel();
        _addColumnNamesPanel.setLayout(new HorizontalLayout());
        _addColumnNamesButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (_columnNames == null) {
                    final ExcelDatastore datastore = createDatastore(getDatastoreName(), getFilename());
                    datastore.openConnection();
                    _columnNames = datastore.getCustomColumnNames();
                }
                final ColumnNamesSetterDialog columnNamesChooserDialog = new ColumnNamesSetterDialog(windowContext,
                        _columnNames);
                columnNamesChooserDialog.setVisible(true);
                columnNamesChooserDialog.addWindowListener(new WindowListener() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        _columnNames = columnNamesChooserDialog.getColumnNames();
                        onSetingsUpdated();
                    }

                    @Override
                    public void windowActivated(WindowEvent e) {

                    }

                    @Override
                    public void windowClosing(WindowEvent e) {

                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {

                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {

                    }

                    @Override
                    public void windowIconified(WindowEvent e) {

                    }

                    @Override
                    public void windowOpened(WindowEvent e) {

                    }
                });
            }
        });
        _addColumnNamesPanel.add(_addColumnNamesButton, 0);
    }

    protected List<Entry<String, JComponent>> getFormElements() {
        List<Entry<String, JComponent>> res = super.getFormElements();
        res.add(new ImmutableEntry<>("Column Names", _addColumnNamesPanel));
        return res;
    }

    @Override
    protected void setFileFilters(AbstractResourceTextField<?> filenameField) {
        FileFilter combinedFilter = FileFilters.combined("Any Excel Spreadsheet (.xls, .xlsx)", FileFilters.XLS,
                FileFilters.XLSX);
        filenameField.addChoosableFileFilter(combinedFilter);
        filenameField.addChoosableFileFilter(FileFilters.XLS);
        filenameField.addChoosableFileFilter(FileFilters.XLSX);
        filenameField.addChoosableFileFilter(FileFilters.ALL);
        filenameField.setSelectedFileFilter(combinedFilter);
        filenameField.addListener(new ResourceTypePresenter.Listener() {
            @Override
            public void onResourceSelected(ResourceTypePresenter<?> presenter, Resource resource) {
                _addColumnNamesButton.setEnabled(true);
            }

            @Override
            public void onPathEntered(ResourceTypePresenter<?> presenter, String path) {

            }
        });
    }

    @Override
    protected String getBannerTitle() {
        return "MS Excel spreadsheet";
    }

    @Override
    public String getWindowTitle() {
        return "Excel spreadsheet | Datastore";
    }

    @Override
    protected ExcelDatastore createDatastore(String name, String filename) {
        return new ExcelDatastore(name, new FileResource(filename), filename, _columnNames);
    }

    @Override
    protected String getDatastoreIconPath() {
        return IconUtils.EXCEL_IMAGEPATH;
    }

    @Override
    protected boolean isPreviewTableEnabled() {
        return true;
    }

    @Override
    protected boolean isPreviewDataAvailable() {
        return showPreview; 
    }
    
    @Override
    protected ExcelDatastore getPreviewDatastore(String filename) {
        return createDatastore(getDatastoreName(), getFilename());
    }

    private void onSetingsUpdated() {
        new SwingWorker<ExcelConfiguration, Void>() {

            @Override
            protected ExcelConfiguration doInBackground() throws Exception {
                if (_columnNames != null && _columnNames.size() > 0) {
                    return new ExcelConfiguration(ExcelConfiguration.DEFAULT_COLUMN_NAME_LINE,
                            new CustomColumnNamingStrategy(_columnNames), true, false);
                } else {
                    return new ExcelConfiguration();
                }
            }

            @Override
            protected void done() {
                try {
                    @SuppressWarnings("unused")
                    final ExcelConfiguration configuration = get();
                    showPreview = true; 
                } catch (Exception e) {
                    final Throwable error = ErrorUtils.unwrapForPresentation(e);
                    setStatusError(error.getMessage());
                     showPreview = false;
                }
                validateAndUpdate();
            }

        }.execute();

    }
}
