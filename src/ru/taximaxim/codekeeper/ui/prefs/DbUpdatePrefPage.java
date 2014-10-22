package ru.taximaxim.codekeeper.ui.prefs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts.DB_UPDATE_PREF;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class DbUpdatePrefPage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    public DbUpdatePrefPage() {
        super(GRID);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        BooleanFieldEditor usePsqlDepcy = new BooleanFieldEditor(
                DB_UPDATE_PREF.USE_PSQL_DEPCY,
                Messages.generalPrefPage_use_psql_depcy_on_generating_script,
                getFieldEditorParent());
        addField(usePsqlDepcy);

        Map<String, String> pref = new HashMap<>();
        pref.put(DB_UPDATE_PREF.DROP_TABLE_STATEMENT, Messages.dBUpdatePrefPage_drop_table);
        pref.put(DB_UPDATE_PREF.ALTER_COLUMN_STATEMENT, Messages.dBUpdatePrefPage_alter_column_statement);
        pref.put(DB_UPDATE_PREF.DROP_COLUMN_STATEMENT, Messages.dBUpdatePrefPage_drop_column_statement);
        GroupFieldsEditor gfe = new GroupFieldsEditor(pref,
                Messages.dBUpdatePrefPage_set_warning_when_next_statements_present,
                getFieldEditorParent());
        addField(gfe);
        
        BooleanFieldEditor showScriptOutputSeparately = new BooleanFieldEditor(
                DB_UPDATE_PREF.SHOW_SCRIPT_OUTPUT_SEPARATELY,
                Messages.dbUpdatePrefPage_show_script_output_in_separate_window, getFieldEditorParent());
        addField(showScriptOutputSeparately);
    }
}