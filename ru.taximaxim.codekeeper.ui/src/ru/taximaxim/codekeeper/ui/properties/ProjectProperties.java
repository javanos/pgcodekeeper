package ru.taximaxim.codekeeper.ui.properties;


import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class ProjectProperties extends PropertyPage {

    private Button btnForceUnixNewlines;
    private Button btnDisableParser;
    private Combo cmbTimezone;
    private CLabel lblWarn;

    private IEclipsePreferences prefs;

    @Override
    public void setElement(IAdaptable element) {
        super.setElement(element);
        prefs = new ProjectScope(element.getAdapter(IProject.class))
                .getNode(UIConsts.PLUGIN_ID.THIS);
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        panel.setLayout(layout);

        btnDisableParser = new Button(panel, SWT.CHECK);
        btnDisableParser.setText(Messages.ProjectProperties_disable_parser_in_external_files);
        btnDisableParser.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, 2, 1));
        btnDisableParser.setSelection(prefs.getBoolean(PROJ_PREF.DISABLE_PARSER_IN_EXTERNAL_FILES, false));

        btnForceUnixNewlines = new Button(panel, SWT.CHECK);
        btnForceUnixNewlines.setText(Messages.ProjectProperties_force_unix_newlines);
        btnForceUnixNewlines.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, 2, 1));
        btnForceUnixNewlines.setSelection(prefs.getBoolean(PROJ_PREF.FORCE_UNIX_NEWLINES, true));

        new Label(panel, SWT.NONE).setText(Messages.projectProperties_timezone_for_all_db_connections);

        cmbTimezone = new Combo(panel, SWT.BORDER | SWT.DROP_DOWN);
        cmbTimezone.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cmbTimezone.setItems(UIConsts.TIME_ZONES.toArray(new String[UIConsts.TIME_ZONES.size()]));
        String tz = prefs.get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC);
        cmbTimezone.setText(tz);
        cmbTimezone.addModifyListener(e -> checkSwitchWarnLbl());

        lblWarn = new CLabel(panel, SWT.NONE);
        lblWarn.setImage(Activator.getEclipseImage(ISharedImages.IMG_OBJS_WARN_TSK));
        lblWarn.setText(Messages.ProjectProperties_change_projprefs_warn);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, false, false, 2, 1);
        gd.exclude = true;
        lblWarn.setLayoutData(gd);
        lblWarn.setVisible(false);

        return panel;
    }

    private void checkSwitchWarnLbl() {
        boolean show = !cmbTimezone.getText()
                .equals(prefs.get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC));
        ((GridData) lblWarn.getLayoutData()).exclude = !show;
        lblWarn.setVisible(show);
        lblWarn.getParent().layout();
    }

    @Override
    protected void performDefaults() {
        btnDisableParser.setSelection(false);
        btnForceUnixNewlines.setSelection(true);
        cmbTimezone.setText(ApgdiffConsts.UTC);
        try {
            fillPrefs();
        } catch (BackingStoreException e) {
            setErrorMessage(MessageFormat.format(
                    Messages.projectProperties_error_occurs_while_saving_properties,
                    e.getLocalizedMessage()));
            setValid(false);
        }
    }

    @Override
    public boolean performOk() {
        try {
            fillPrefs();
        } catch (BackingStoreException e) {
            setErrorMessage(MessageFormat.format(
                    Messages.projectProperties_error_occurs_while_saving_properties,
                    e.getLocalizedMessage()));
            setValid(false);
            return false;
        }
        return true;
    }

    private void fillPrefs() throws BackingStoreException {
        prefs.putBoolean(PROJ_PREF.DISABLE_PARSER_IN_EXTERNAL_FILES, btnDisableParser.getSelection());
        prefs.putBoolean(PROJ_PREF.FORCE_UNIX_NEWLINES, btnForceUnixNewlines.getSelection());
        prefs.put(PROJ_PREF.TIMEZONE, cmbTimezone.getText());
        prefs.flush();
        setValid(true);
        setErrorMessage(null);
    }
}
