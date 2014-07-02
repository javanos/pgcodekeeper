package ru.taximaxim.codekeeper.ui.dbstore;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class DbPicker extends Group {

    final private boolean allowShellResize;
    
    private Label lblFieldName;
    private Button btnStorePick;
    private CLabel lblWarnDbPass;
    
    private LocalResourceManager lrm;
    
    public Label lblName;
    public Text txtDbName, txtDbUser, txtDbPass, txtDbHost, txtDbPort;
    
    /**
     * Constructs a control that is allowed to modify its shell size.
     */
    public DbPicker(Composite parent, int style, final IPreferenceStore prefStore) {
        this(parent, style, prefStore, true);
    }
    
    public DbPicker(Composite parent, int style, final IPreferenceStore prefStore,
            boolean allowShellResize) {
        super(parent, style);
        setLayout(new GridLayout(4, false));
        
        this.allowShellResize = allowShellResize;
        this.lrm = new LocalResourceManager(JFaceResources.getResources(), this);
        
        lblFieldName = new Label(this, SWT.NONE);
        lblFieldName.setText(Messages.DbPicker_entry_name);
        GridData gd = new GridData();
        gd.exclude = true;
        lblFieldName.setLayoutData(gd);
        lblFieldName.setVisible(false);
        
        lblName = new Label(this, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false, 3, 1);
        gd.exclude = true;
        lblName.setLayoutData(gd);
        lblName.setVisible(false);
        
        new Label(this, SWT.NONE).setText(Messages.DbPicker_db_name);
        
        txtDbName = new Text(this, SWT.BORDER);
        txtDbName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        btnStorePick = new Button(this, SWT.PUSH);
        btnStorePick.setText("..."); //$NON-NLS-1$
        btnStorePick.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        btnStorePick.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DbStorePickerDialog dialog = new DbStorePickerDialog(getShell(), prefStore);
                dialog.openAndSetText(txtDbName, txtDbUser, txtDbPass, txtDbHost, txtDbPort);
            }
        });
        
        new Label(this, SWT.NONE).setText(Messages.DbPicker_db_user);
        
        txtDbUser = new Text(this, SWT.BORDER);
        txtDbUser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        
        new Label(this, SWT.NONE).setText(Messages.DbPicker_db_password);
        
        txtDbPass = new Text(this, SWT.BORDER | SWT.PASSWORD);
        txtDbPass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        txtDbPass.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                GridData gd = (GridData) lblWarnDbPass.getLayoutData();
                
                if((txtDbPass.getText().isEmpty() && !gd.exclude)
                        || (!txtDbPass.getText().isEmpty() && gd.exclude)) {
                    lblWarnDbPass.setVisible(!lblWarnDbPass.getVisible());
                    gd.exclude = !gd.exclude;
                    
                    layout();
                }
            }
        });
        
        lblWarnDbPass = new CLabel(this, SWT.NONE);
        lblWarnDbPass.setImage(lrm.createImage(ImageDescriptor.createFromURL(
                Activator.getContext().getBundle().getResource(
                        UIConsts.FILENAME_ICONWARNING))));
        lblWarnDbPass.setText(Messages.DbPicker_warning
                + Messages.DbPicker_providing_password_here_is_insecure
                + Messages.DbPicker_consider_using_pgpass_file_instead);
        gd = new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1);
        gd.exclude = true;
        lblWarnDbPass.setLayoutData(gd);
        lblWarnDbPass.setVisible(false);
        
        new Label(this, SWT.NONE).setText(Messages.DbPicker_db_host);
        
        txtDbHost = new Text(this, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 80;
        txtDbHost.setLayoutData(gd);
        
        new Label(this, SWT.NONE).setText(Messages.DbPicker_port);
        
        txtDbPort = new Text(this, SWT.BORDER);
        gd = new GridData(60, SWT.DEFAULT);
        txtDbPort.setLayoutData(gd);
    }
    
    /**
     * Remove storePicker button and show Entry Name field.
     */
    public void setStoreEditMode() {
        ((GridData) lblFieldName.getLayoutData()).exclude = false;
        lblFieldName.setVisible(true);
        
        ((GridData) lblName.getLayoutData()).exclude = false;
        lblName.setVisible(true);
        
        ((GridData) btnStorePick.getLayoutData()).exclude = true;
        btnStorePick.setVisible(false);
        
        txtDbName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        
        layout();
    }
    
    @Override
    public void layout() {
        if(allowShellResize) {
            getShell().pack();
            layout(false);
        } else {
            getShell().layout(true, true);
        }
    }
        
    @Override
    protected void checkSubclass() {
        // allow subclassing, we just use Group as a Composite
        // ~should~ be fine
    }
}