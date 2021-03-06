package ru.taximaxim.codekeeper.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

import cz.startnet.utils.pgdiff.PgCodekeeperException;
import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.fileutils.FileUtils;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.IgnoreList;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeFlattener;
import ru.taximaxim.codekeeper.apgdiff.model.graph.DepcyTreeExtender;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.COMMAND;
import ru.taximaxim.codekeeper.ui.UIConsts.COMMIT_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.CONTEXT;
import ru.taximaxim.codekeeper.ui.UIConsts.DB_UPDATE_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.EDITOR;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.UIConsts.PERSPECTIVE;
import ru.taximaxim.codekeeper.ui.UIConsts.PG_EDIT_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PLUGIN_ID;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PATH;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.UiSync;
import ru.taximaxim.codekeeper.ui.consoles.ConsoleFactory;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.dialogs.CommitDialog;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.dialogs.ManualDepciesDialog;
import ru.taximaxim.codekeeper.ui.differ.ClassicTreeDiffer;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.differ.DiffPaneViewer;
import ru.taximaxim.codekeeper.ui.differ.DiffTableViewer;
import ru.taximaxim.codekeeper.ui.differ.Differ;
import ru.taximaxim.codekeeper.ui.differ.TreeDiffer;
import ru.taximaxim.codekeeper.ui.fileutils.FileUtilsUi;
import ru.taximaxim.codekeeper.ui.fileutils.ProjectUpdater;
import ru.taximaxim.codekeeper.ui.handlers.OpenProjectUtils;
import ru.taximaxim.codekeeper.ui.job.SingletonEditorJob;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;
import ru.taximaxim.codekeeper.ui.pgdbproject.parser.PgUIDumpLoader;
import ru.taximaxim.codekeeper.ui.prefs.ignoredobjects.InternalIgnoreList;
import ru.taximaxim.codekeeper.ui.propertytests.ChangesJobTester;
import ru.taximaxim.codekeeper.ui.sqledit.SQLEditor;
import ru.taximaxim.codekeeper.ui.views.DBPair;

public class ProjectEditorDiffer extends EditorPart implements IResourceChangeListener {

    private final IPreferenceStore mainPrefs = Activator.getDefault().getPreferenceStore();

    private PgDbProject proj;
    private ProjectEditorSelectionProvider sp;
    private Composite parent;

    private Object currentRemote;
    private DbSource dbProject;
    private DbSource dbRemote;
    private TreeElement diffTree;
    private Object loadedRemote;

    private Composite contNotifications;
    private Label lblNotificationText;
    private Button btnDismissRefresh;
    private Button btnGetChanges;

    private DiffTableViewer diffTable;
    private DiffPaneViewer diffPane;

    private LocalResourceManager lrm;
    private boolean isDBLoaded;
    private boolean isCommitCommandAvailable;
    private List<Entry<PgStatement, PgStatement>> manualDepciesSource = new LinkedList<>();
    private List<Entry<PgStatement, PgStatement>> manualDepciesTarget = new LinkedList<>();

    public IProject getProject() {
        return proj.getProject();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof ProjectEditorInput)) {
            throw new PartInitException(Messages.ProjectEditorDiffer_error_bad_input_type);
        }

        ProjectEditorInput in = (ProjectEditorInput) input;
        Exception ex = in.getError();
        if (ex != null) {
            throw new PartInitException(in.getError().getLocalizedMessage(), ex);
        }

        setInput(input);
        setSite(site);
        setPartName(in.getName());

        proj = new PgDbProject(in.getProject());
        sp = new ProjectEditorSelectionProvider(proj.getProject());

        // message box
        if(!site.getPage().getPerspective().getId().equals(PERSPECTIVE.MAIN)){
            askPerspectiveChange(site);
        }
        getSite().setSelectionProvider(sp);
    }

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;

        parent.setLayout(new GridLayout());
        lrm = new LocalResourceManager(JFaceResources.getResources(), parent);

        // notifications container
        // simplified for 1 static notification
        // refactor into multiple child composites w/ description class
        // for multiple dynamic notifications if necessary
        contNotifications = new Group(parent, SWT.NONE);
        contNotifications.setLayout(new GridLayout(4, false));

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.exclude = true;
        contNotifications.setVisible(false);
        contNotifications.setLayoutData(gd);

        Label lblWarnIcon = new Label(contNotifications, SWT.NONE);
        lblWarnIcon.setImage(Activator.getEclipseImage(ISharedImages.IMG_OBJS_WARN_TSK));
        lblWarnIcon.setLayoutData(new GridData(SWT.DEFAULT, SWT.BOTTOM, false, true));

        Label lblNotification = new Label(contNotifications, SWT.NONE);
        lblNotification.setText(Messages.DiffPresentationPane_attention);
        lblNotification.setLayoutData(new GridData(SWT.DEFAULT, SWT.BOTTOM, false, true));
        lblNotification.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
        lblNotification.setFont(lrm.createFont(FontDescriptor.createFrom(
                lblNotification.getFont()).withStyle(SWT.BOLD).increaseHeight(2)));

        lblNotificationText = new Label(contNotifications, SWT.NONE);
        lblNotificationText.setLayoutData(new GridData(SWT.DEFAULT, SWT.BOTTOM, false, true));

        btnDismissRefresh = new Button(contNotifications, SWT.PUSH | SWT.FLAT);
        btnDismissRefresh.setImage(lrm.createImage(ImageDescriptor.createFromURL(
                Activator.getContext().getBundle().getResource(FILE.ICONCLOSE))));
        btnDismissRefresh.setToolTipText(Messages.DiffPresentationPane_dismiss);
        btnDismissRefresh.setLayoutData(new GridData(SWT.DEFAULT, SWT.BOTTOM, false, true));

        btnDismissRefresh.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                hideNotificationArea();
            }
        });
        // end notifications container

        SashForm sashOuter = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
        sashOuter.setLayoutData(new GridData(GridData.FILL_BOTH));

        IStatusLineManager manager = getEditorSite().getActionBars().getStatusLineManager();

        diffTable = new DiffTableViewer(sashOuter, false, manager, Paths.get(proj.getProject().getLocationURI())) {

            @Override
            public void createRightSide(Composite container) {
                GridLayout layout = new GridLayout(4, false);
                layout.marginHeight = 0;
                layout.marginWidth = 0;
                container.setLayout(layout);

                Label l = new Label(container, SWT.NONE);
                l.setForeground(l.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
                l.setText(Messages.DiffTableViewer_apply_to);
                l.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

                Button btnCommit = new Button(container, SWT.PUSH);
                btnCommit.setText(Messages.DiffTableViewer_to_project);
                btnCommit.setImage(lrm.createImage(ImageDescriptor.createFromURL(Activator.getContext()
                        .getBundle().getResource(FILE.ICONAPPSMALL))));
                btnCommit.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        try {
                            commit();
                        } catch (PgCodekeeperException ex) {
                            ExceptionNotifier.notifyDefault(Messages.error_creating_dependency_graph, ex);
                        }
                    }
                });

                Button btnDiff = new Button(container, SWT.PUSH);
                btnDiff.setText(Messages.DiffTableViewer_to_database);
                btnDiff.setImage(lrm.createImage(ImageDescriptor.createFromURL(Activator.getContext()
                        .getBundle().getResource(FILE.ICONDATABASE))));
                btnDiff.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        diff();
                    }
                });

                btnGetChanges = new Button(container, SWT.PUSH);
                btnGetChanges.setImage(lrm.createImage(ImageDescriptor.createFromURL(Activator.getContext()
                        .getBundle().getResource(FILE.ICONREFRESH))));
                btnGetChanges.setText(Messages.DiffTableViewer_get_changes);
                btnGetChanges.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        getChanges();
                    }
                });
            }
        };

        diffTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        diffTable.getViewer().addPostSelectionChangedListener(e -> {
            IStructuredSelection selection = (IStructuredSelection) e.getSelection();
            if (selection.size() != 1) {
                diffPane.setInput(null, null);
            } else {
                TreeElement el = (TreeElement) selection.getFirstElement();
                diffPane.setInput(el, diffTable.getElements());
            }
        });

        diffTable.getViewer().addDoubleClickListener(
                e -> openElementInEditor((TreeElement) ((IStructuredSelection) e.getSelection()).getFirstElement()));

        diffTable.getViewer().addPostSelectionChangedListener(
                e -> sp.fireSelectionChanged(e, new DBPair(dbProject, dbRemote)));

        diffPane = new DiffPaneViewer(sashOuter, SWT.NONE);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE
                | IResourceChangeEvent.POST_CHANGE);

        ICommandService commandService =
                PlatformUI.getWorkbench().getService(ICommandService.class);
        @SuppressWarnings("unchecked")
        Collection<String> commandIds = commandService.getDefinedCommandIds();
        isCommitCommandAvailable = commandIds.contains(COMMAND.COMMIT_COMMAND_ID);

        getSite().getService(IContextService.class).activateContext(CONTEXT.MAIN);
    }

    public void addDependency() {
        if (isDBLoaded){
            ManualDepciesDialog dialog = new ManualDepciesDialog(parent.getShell(),
                    manualDepciesSource, manualDepciesTarget,
                    PgDatabase.listPgObjects(dbRemote.getDbObject()),
                    PgDatabase.listPgObjects(dbProject.getDbObject()),
                    Messages.database, Messages.ProjectEditorDiffer_project);
            if (dialog.open() == Dialog.OK) {
                manualDepciesSource = dialog.getDepciesSourceList();
                manualDepciesTarget = dialog.getDepciesTargetList();
            }
        }
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void setFocus() {
        parent.setFocus();
        diffTable.updateObjectsLabels();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // no impl
    }

    @Override
    public void doSaveAs() {
        // no impl
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        switch (event.getType()) {
        case IResourceChangeEvent.PRE_CLOSE:
        case IResourceChangeEvent.PRE_DELETE:
            handlerCloseProject(event);
            break;
        case IResourceChangeEvent.POST_CHANGE:
            handleChangeProject(event.getDelta());
            break;
        default:
            break;
        }
    }

    private void handlerCloseProject(IResourceChangeEvent event) {
        if (event.getResource().getProject().equals(proj.getProject())) {
            UiSync.exec(parent, () -> {
                if (!parent.isDisposed()) {
                    getSite().getPage().closeEditor(ProjectEditorDiffer.this, true);
                }
            });
        }
    }

    private void handleChangeProject(IResourceDelta rootDelta) {
        final boolean[] schemaChanged = new boolean[1];
        try {
            rootDelta.accept(delta -> {
                if (schemaChanged[0]) {
                    return false;
                }
                // something other than just markers has changed
                // check that it's our resource
                if (delta.getFlags() != IResourceDelta.MARKERS &&
                        PgUIDumpLoader.isInProject(delta) &&
                        delta.getResource().getType() == IResource.FILE &&
                        delta.getResource().getProject().equals(proj.getProject())) {
                    schemaChanged[0] = true;
                    return false;
                }
                return true;
            });
        } catch (CoreException ex) {
            Log.log(ex);
        }

        if (schemaChanged[0]) {
            UiSync.exec(parent, this::notifyProjectChanged);
        }
    }

    public void getChanges(){
        Object currentRemote = this.currentRemote;
        if (currentRemote == null) {
            MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_INFORMATION);
            mb.setText(Messages.GetChanges_select_source);
            mb.setMessage(Messages.GetChanges_select_source_msg);
            mb.open();
            return;
        }

        String charset;
        try {
            charset = proj.getProjectCharset();
        } catch (CoreException e) {
            ExceptionNotifier.notifyDefault(Messages.DiffPresentationPane_error_loading_changes, e);
            return;
        }
        IEclipsePreferences projProps = proj.getPrefs();

        boolean forceUnixNewlines = projProps.getBoolean(PROJ_PREF.FORCE_UNIX_NEWLINES, true);

        DbSource dbProject = DbSource.fromProject(proj);

        DbSource dbRemote;
        TreeDiffer newDiffer;
        String name;

        if (currentRemote instanceof DbInfo) {
            DbInfo dbInfo = (DbInfo) currentRemote;
            newDiffer = TreeDiffer.getTree(dbProject, dbInfo, charset, forceUnixNewlines,
                    mainPrefs, projProps.get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC));
            name = dbInfo.getName();
            saveLastDb(dbInfo);
        } else {
            File file = (File) currentRemote;
            name = file.getName();
            dbRemote = DbSource.fromFile(forceUnixNewlines, file, charset);
            newDiffer = new ClassicTreeDiffer(dbProject, dbRemote, false);
        }

        String title = getEditorInput().getName() + " - " + name; //$NON-NLS-1$
        ((ProjectEditorInput)getEditorInput()).setToolTipText(title);
        setPartName(title);

        if (!OpenProjectUtils.checkVersionAndWarn(proj.getProject(), parent.getShell(), true)) {
            return;
        }

        Log.log(Log.LOG_INFO, "Getting changes for diff"); //$NON-NLS-1$

        reset();
        hideNotificationArea();

        Job job = new SingletonEditorJob(Messages.diffPresentationPane_getting_changes_for_diff,
                this, ChangesJobTester.EVAL_PROP) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    SubMonitor sub = SubMonitor.convert(monitor,
                            Messages.diffPresentationPane_getting_changes_for_diff, 100);
                    proj.getProject().refreshLocal(IResource.DEPTH_INFINITE, sub.newChild(10));

                    PgDiffUtils.checkCancelled(monitor);
                    sub.subTask(Messages.diffPresentationPane_getting_changes_for_diff);
                    newDiffer.run(sub.newChild(90));
                    monitor.done();
                } catch (InvocationTargetException | CoreException e) {
                    return new Status(Status.ERROR, PLUGIN_ID.THIS,
                            Messages.error_in_differ_thread, e);
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void aboutToRun(IJobChangeEvent event) {
                UiSync.exec(parent, () -> {
                    if (!parent.isDisposed()) {
                        btnGetChanges.setEnabled(false);
                    }
                });
            }


            @Override
            public void done(IJobChangeEvent event) {
                UiSync.exec(parent, () -> {
                    if (!parent.isDisposed()) {
                        btnGetChanges.setEnabled(true);
                    }
                });

                if (event.getResult().isOK()) {
                    UiSync.exec(parent, () -> {
                        if (!parent.isDisposed()) {
                            loadedRemote = currentRemote;
                            setInput(newDiffer.getDbSource(), newDiffer.getDbTarget(),
                                    newDiffer.getDiffTree());
                        }
                    });
                }

                newDiffer.getErrors().forEach(e -> StatusManager.getManager().handle(
                        new Status(IStatus.WARNING, PLUGIN_ID.THIS, e.toString()),
                        StatusManager.SHOW));
            }
        });
        job.setUser(true);
        job.schedule();
    }

    private void askPerspectiveChange(IEditorSite site) {
        String mode = mainPrefs.getString(PG_EDIT_PREF.PERSPECTIVE_CHANGING_STATUS);
        // if select "YES" with toggle
        if (mode.equals(MessageDialogWithToggle.ALWAYS)){
            changePerspective(site);
            // if not select "NO" with toggle, show choice message dialog
        } else if (!mode.equals(MessageDialogWithToggle.NEVER)){
            MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(site.getShell(),
                    Messages.change_perspective_title, Messages.change_perspective_message,
                    Messages.remember_choice_toggle, false, mainPrefs, PG_EDIT_PREF.PERSPECTIVE_CHANGING_STATUS);
            if(dialog.getReturnCode() == IDialogConstants.YES_ID){
                changePerspective(site);
            }
        }
    }

    private void changePerspective(IEditorSite site) {
        //change perspective to pgCodeKeeper
        try {
            site.getWorkbenchWindow().getWorkbench().showPerspective(PERSPECTIVE.MAIN,
                    site.getWorkbenchWindow());
        } catch (WorkbenchException e) {
            Log.log(Log.LOG_ERROR, "Can't change perspective", e); //$NON-NLS-1$
        }
    }

    private void openElementInEditor(TreeElement el) {
        if (el != null && el.getSide() != DiffSide.RIGHT) {
            try {
                FileUtilsUi.openFileInSqlEditor(el.getPgStatement(dbProject.getDbObject()).getLocation());
            } catch (PartInitException e) {
                ExceptionNotifier.notifyDefault(e.getLocalizedMessage(), e);
            }
        }
    }
    /**
     * @param remote remote DB schema: either {@link File} or {@link DbInfo}
     * @throws IllegalArgumentException invalid remote type
     */
    public void setCurrentDb(Object currentRemote) {
        if (currentRemote == null || currentRemote instanceof DbInfo || currentRemote instanceof File) {
            this.currentRemote = currentRemote;
        } else {
            throw new IllegalArgumentException("Remote is not a File or DbInfo!"); //$NON-NLS-1$
        }
    }

    public Object getCurrentDb() {
        if (currentRemote != null) {
            return currentRemote;
        }

        return DbInfo.getLastDb(proj.getPrefs().get(PROJ_PREF.LAST_DB_STORE, "")); //$NON-NLS-1$
    }

    public void saveLastDb(DbInfo lastDb) {
        saveLastDb(lastDb, getProject());
    }

    public static void saveLastDb(DbInfo lastDb, IProject project) {
        IEclipsePreferences prefs = PgDbProject.getPrefs(project);
        if (prefs != null) {
            prefs.put(PROJ_PREF.LAST_DB_STORE, lastDb.getName());
            try {
                prefs.flush();
            } catch (BackingStoreException ex) {
                Log.log(ex);
            }
        }
    }

    public void diff() {
        Log.log(Log.LOG_INFO, "Started DB update"); //$NON-NLS-1$
        if (warnCheckedElements() < 1 ||
                !OpenProjectUtils.checkVersionAndWarn(proj.getProject(), parent.getShell(), true)) {
            return;
        }

        IEclipsePreferences pref = proj.getPrefs();
        final Differ differ = new Differ(dbRemote.getDbObject(),
                dbProject.getDbObject(), diffTree.getRevertedCopy(), false,
                pref.get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC));
        differ.setAdditionalDepciesSource(manualDepciesSource);
        differ.setAdditionalDepciesTarget(manualDepciesTarget);

        Job job = differ.getDifferJob();
        job.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
                Log.log(Log.LOG_INFO, "Differ job finished with status " +  //$NON-NLS-1$
                        event.getResult().getSeverity());
                if (event.getResult().isOK()) {
                    UiSync.exec(parent, () -> {
                        if (!parent.isDisposed()) {
                            try {
                                showEditor(differ);
                            } catch (PartInitException ex) {
                                ExceptionNotifier.notifyDefault(
                                        Messages.ProjectEditorDiffer_error_opening_script_editor, ex);
                            }
                        }
                    });
                }
            }
        });
        job.setUser(true);
        job.schedule();
    }

    private void setInput(DbSource dbProject, DbSource dbRemote, TreeElement diffTree) {
        this.dbProject = dbProject;
        this.dbRemote = dbRemote;
        this.diffTree = diffTree;
        diffPane.setDbSources(dbProject, dbRemote);
        diffPane.setInput(null, null);

        IgnoreList ignoreList = null;
        if (diffTree != null) {
            ignoreList = InternalIgnoreList.readInternalList();
            InternalIgnoreList.readAppendList(
                    proj.getPathToProject().resolve(FILE.IGNORED_OBJECTS), ignoreList);

            if (loadedRemote != null && loadedRemote instanceof DbInfo) {
                ((DbInfo)loadedRemote).appendIgnoreFiles(ignoreList);
            }
        }
        diffTable.setInput(dbProject, dbRemote, diffTree, ignoreList);
        if (diffTree != null) {
            isDBLoaded = true;
            manualDepciesSource.clear();
            manualDepciesTarget.clear();
        }
    }

    private void reset() {
        isDBLoaded = false;
        manualDepciesSource.clear();
        manualDepciesTarget.clear();
        setInput(null, null, null);
    }

    private void hideNotificationArea() {
        showNotificationArea(false, null);
    }

    private void showEditor(Differ differ) throws PartInitException {
        if (differ.getScript().isDangerDdl(
                !mainPrefs.getBoolean(DB_UPDATE_PREF.DROP_TABLE_STATEMENT),
                !mainPrefs.getBoolean(DB_UPDATE_PREF.ALTER_COLUMN_STATEMENT),
                !mainPrefs.getBoolean(DB_UPDATE_PREF.DROP_COLUMN_STATEMENT),
                !mainPrefs.getBoolean(DB_UPDATE_PREF.RESTART_WITH_STATEMENT))){
            MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
            mb.setText(Messages.sqlScriptDialog_warning);
            mb.setMessage(Messages.sqlScriptDialog_script_contains_statements_that_may_modify_data);
            if (mb.open() != SWT.OK){
                return;
            }
        }

        try {
            boolean inProj = false;
            String creationMode = mainPrefs.getString(DB_UPDATE_PREF.CREATE_SCRIPT_IN_PROJECT);
            // if select "YES" with toggle
            if (creationMode.equals(MessageDialogWithToggle.ALWAYS)) {
                inProj = true;
                // if not select "NO" with toggle, show choice message dialog
            } else if (!creationMode.equals(MessageDialogWithToggle.NEVER)) {
                MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(parent.getShell(),
                        Messages.ProjectEditorDiffer_script_creation_title, Messages.ProjectEditorDiffer_script_creation_message,
                        Messages.remember_choice_toggle, false, mainPrefs, DB_UPDATE_PREF.CREATE_SCRIPT_IN_PROJECT);
                if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
                    inProj = true;
                }
            }

            String content = differ.getDiffDirect();
            String filename = generateScriptName();
            if (inProj) {
                IEditorInput file = createProjectScriptFile(content, filename);
                if (loadedRemote instanceof DbInfo) {
                    SQLEditor.saveLastDb((DbInfo) loadedRemote, file);
                }
                getSite().getPage().openEditor(file, EDITOR.SQL);
            } else {
                FileUtilsUi.saveOpenTmpSqlEditor(content, filename);
            }
        } catch (CoreException | IOException ex) {
            ExceptionNotifier.notifyDefault(
                    Messages.ProjectEditorDiffer_error_creating_file, ex);
        }
    }

    private String generateScriptName() {
        String name = FileUtils.FILE_DATE.format(LocalDateTime.now()) + " migration"; //$NON-NLS-1$
        if (loadedRemote != null) {
            name += " for " + getRemoteName(loadedRemote); //$NON-NLS-1$
        }
        return FileUtils.sanitizeFilename(name);
    }

    private IEditorInput createProjectScriptFile(String content, String filename) throws CoreException, IOException {
        Log.log(Log.LOG_INFO, "Creating file " + filename); //$NON-NLS-1$
        IProject iProject = proj.getProject();
        IFolder folder = iProject.getFolder(PROJ_PATH.MIGRATION_DIR);
        if (!folder.exists()){
            folder.create(IResource.NONE, true, null);
        }
        IFile file = folder.getFile(filename + ".sql"); //$NON-NLS-1$
        InputStream source = new ByteArrayInputStream(content.getBytes(proj.getProjectCharset()));
        file.create(source, IResource.NONE, null);
        return new FileEditorInput(iProject.getFile(file.getProjectRelativePath()));
    }

    private void showNotificationArea(boolean visible, String message) {
        if (diffTree == null && visible) {
            // since there's only one notification about diff sides changing
            // we can skip showing it if the pane is empty (has no diff loaded)
            return;
        }
        if (message != null) {
            lblNotificationText.setText(message);
        }
        ((GridData) contNotifications.getLayoutData()).exclude = !visible;
        contNotifications.setVisible(visible);
        parent.layout();
        if (visible) {
            btnDismissRefresh.setFocus();
        }
    }

    private void notifyProjectChanged() {
        if (!parent.isDisposed()) {
            showNotificationArea(true, Messages.DiffPresentationPane_project_modified);
            reset();
        }
    }

    public void commit() throws PgCodekeeperException {
        Log.log(Log.LOG_INFO, "Started project update"); //$NON-NLS-1$
        if (warnCheckedElements() < 1
                || !OpenProjectUtils.checkVersionAndWarn(proj.getProject(), parent.getShell(), true)
                || warnLibChange()) {
            return;
        }

        boolean considerDepcy = mainPrefs.getBoolean(COMMIT_PREF.CONSIDER_DEPCY_IN_COMMIT);
        Set<TreeElement> sumNewAndDelete = null;
        if(considerDepcy){
            Log.log(Log.LOG_INFO, "Processing depcies for project update"); //$NON-NLS-1$
            sumNewAndDelete = new DepcyTreeExtender(dbProject.getDbObject(),
                    dbRemote.getDbObject(), diffTree).getDepcies();
        }

        Log.log(Log.LOG_INFO, "Querying user for project update"); //$NON-NLS-1$
        // display commit dialog
        CommitDialog cd = new CommitDialog(parent.getShell(), sumNewAndDelete, dbProject, dbRemote,
                diffTree, mainPrefs, isCommitCommandAvailable);
        if (cd.open() != CommitDialog.OK) {
            return;
        }

        Log.log(Log.LOG_INFO, "Updating project " + proj.getProjectName()); //$NON-NLS-1$
        Job job = new JobProjectUpdater(Messages.projectEditorDiffer_save_project, diffTree);
        job.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
                Log.log(Log.LOG_INFO, "Project updater job finished with status " + //$NON-NLS-1$
                        event.getResult().getSeverity());
                if (event.getResult().isOK()) {
                    ConsoleFactory.write(Messages.commitPartDescr_success_project_updated);
                    try {
                        proj.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
                        UiSync.exec(parent, () -> {
                            if (!parent.isDisposed()) {
                                callEgitCommitCommand();
                            }
                        });
                    } catch (CoreException e) {
                        ExceptionNotifier.notifyDefault(Messages.ProjectEditorDiffer_error_refreshing_project, e);
                    }
                }
            }
        });

        job.setUser(true);
        job.schedule();
    }

    private void callEgitCommitCommand(){
        if (!isCommitCommandAvailable || !mainPrefs.getBoolean(PREF.CALL_COMMIT_COMMAND_AFTER_UPDATE)){
            return;
        }
        try {
            getSite().getSelectionProvider().setSelection(new StructuredSelection(proj.getProject()));
            getSite().getService(IHandlerService.class).executeCommand(COMMAND.COMMIT_COMMAND_ID, null);
        } catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
            Log.log(Log.LOG_WARNING, "Could not execute command " + COMMAND.COMMIT_COMMAND_ID, e); //$NON-NLS-1$
            ExceptionNotifier.notifyDefault(Messages.ProjectEditorDiffer_failed_egit_commit, e);
        }
    }

    private void resetRemoteChanged() {
        // may be called off UI thread so check that we're still alive
        if (!parent.isDisposed()) {
            showNotificationArea(true, Messages.DiffPresentationPane_remote_changed_notification);
            reset();
        }
    }

    public void updateRemoteChanged() {
        // may be called off UI thread so check that we're still alive
        if (!parent.isDisposed()) {
            getChanges();
            showNotificationArea(true, Messages.DiffPresentationPane_remote_changed_notification);
        }
    }

    /**
     * @return number of checked elements
     */
    private int warnCheckedElements() {
        int checked = diffTable.getCheckedElementsCount();
        if (checked < 1) {
            MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_INFORMATION);
            mb.setMessage(Messages.please_check_at_least_one_row);
            mb.setText(Messages.empty_selection);
            mb.open();
        }
        return checked;
    }

    private boolean warnLibChange() {
        if (diffTable.checkLibChange()) {
            if (proj.getPrefs().getBoolean(PROJ_PREF.LIB_SAFE_MODE, true)) {
                MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_INFORMATION);
                mb.setMessage(Messages.ProjectEditorDiffer_lib_change_error_message);
                mb.setText(Messages.ProjectEditorDiffer_lib_change_warning_title);
                mb.open();
                return true;
            }

            MessageBox mb = new MessageBox(getEditorSite().getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mb.setText(Messages.ProjectEditorDiffer_lib_change_warning_title);
            mb.setMessage(Messages.ProjectEditorDiffer_lib_change_warning_message);
            return mb.open() != SWT.YES;
        }
        return false;
    }

    private class JobProjectUpdater extends Job {

        private final TreeElement tree;

        JobProjectUpdater(String name, TreeElement tree) {
            super(name);
            this.tree = tree;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            SubMonitor pm = SubMonitor.convert(
                    monitor, Messages.commitPartDescr_commiting, 2);

            Log.log(Log.LOG_INFO, "Applying diff tree to db"); //$NON-NLS-1$
            pm.newChild(1).subTask(Messages.commitPartDescr_modifying_db_model); // 1
            pm.newChild(1).subTask(Messages.commitPartDescr_exporting_db_model); // 2

            try {
                Collection<TreeElement> checked = new TreeFlattener()
                        .onlySelected()
                        .onlyEdits(dbProject.getDbObject(), dbRemote.getDbObject())
                        .flatten(tree);
                new ProjectUpdater(dbRemote.getDbObject(), dbProject.getDbObject(),
                        checked, proj).updatePartial();
                monitor.done();
            } catch (IOException | CoreException e) {
                return new Status(Status.ERROR, PLUGIN_ID.THIS,
                        Messages.ProjectEditorDiffer_commit_error, e);
            }
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            return Status.OK_STATUS;
        }
    }

    public static void notifyDbChanged(DbInfo dbinfo) {
        String action = Activator.getDefault().getPreferenceStore().getString(PG_EDIT_PREF.EDITOR_UPDATE_ACTION);
        if (action.equals(PG_EDIT_PREF.NO_ACTION)) {
            return;
        }
        for (IWorkbenchWindow wnd : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : wnd.getPages()) {
                for (IEditorReference ref : page.getEditorReferences()) {
                    IEditorPart ed = ref.getEditor(false);
                    if (ed instanceof ProjectEditorDiffer) {
                        notifyDbChanged(dbinfo, (ProjectEditorDiffer) ed, action.equals(PG_EDIT_PREF.UPDATE));
                    }
                }
            }
        }
    }

    private static void notifyDbChanged(DbInfo dbinfo, ProjectEditorDiffer editor, boolean update) {
        UiSync.exec(editor.parent, () -> {
            if (dbinfo.equals(editor.currentRemote)) {
                if (update) {
                    editor.updateRemoteChanged();
                } else {
                    editor.resetRemoteChanged();
                }
            }
        });
    }

    private static String getRemoteName(Object remote) {
        if (remote instanceof DbInfo) {
            return ((DbInfo) remote).getName();
        } else if (remote instanceof File) {
            return ((File) remote).getName();
        } else {
            throw new IllegalArgumentException("Remote is not a File or DbInfo!"); //$NON-NLS-1$
        }
    }
}