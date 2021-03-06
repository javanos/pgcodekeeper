package ru.taximaxim.codekeeper.ui.pgdbproject.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.loader.PgDumpLoader;
import cz.startnet.utils.pgdiff.parsers.antlr.AntlrError;
import cz.startnet.utils.pgdiff.parsers.antlr.StatementBodyContainer;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts.WORK_DIR_NAMES;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.model.exporter.ModelExporter;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.MARKER;
import ru.taximaxim.codekeeper.ui.UIConsts.NATURE;

/**
 * {@link PgDumpLoader} extension that works with workspace {@link IResource} structure
 * instead of actual file system.<br>
 * Converts ANTLR parsing errors to {@link IMarker}s for {@link IResource}s.
 */
public class PgUIDumpLoader extends PgDumpLoader {

    private final IFile file;

    public PgUIDumpLoader(IFile ifile, PgDiffArguments args, IProgressMonitor monitor, int monitoringLevel)
            throws CoreException {
        super(ifile.getContents(), ifile.getLocation().toOSString(), args, monitor, monitoringLevel);
        file = ifile;
    }

    /**
     * This constructor sets the monitoring level to the default of 1.
     * @throws CoreException
     */
    public PgUIDumpLoader(IFile ifile, PgDiffArguments args, IProgressMonitor monitor)
            throws CoreException {
        this(ifile, args, monitor, 1);
    }

    /**
     * This constructor uses {@link NullProgressMonitor}.
     * @throws CoreException
     */
    public PgUIDumpLoader(IFile ifile, PgDiffArguments args) throws CoreException {
        this(ifile, args, new NullProgressMonitor(), 0);
    }

    public PgDatabase loadFile(PgDatabase db) throws InterruptedException, IOException {
        try {
            load(db);
            return db;
        } finally {
            updateMarkers();
        }
    }

    private void updateMarkers() {
        try {
            file.deleteMarkers(MARKER.ERROR, false, IResource.DEPTH_ZERO);
        } catch (CoreException ex) {
            Log.log(ex);
        }
        IDocument doc = null;
        for (AntlrError antlrError : getErrors()) {
            try {
                IMarker marker = file.createMarker(MARKER.ERROR);
                int line = antlrError.getLine();
                marker.setAttribute(IMarker.LINE_NUMBER, line);
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                marker.setAttribute(IMarker.MESSAGE, antlrError.getMsg());
                int start = antlrError.getStart();
                int stop = antlrError.getStop();
                if (start == -1 || stop == -1) {
                    if (doc == null) {
                        // load only when this case actually happens
                        IDocumentProvider provider = new TextFileDocumentProvider();
                        provider.connect(file);
                        doc = provider.getDocument(file);
                    }
                    int lineOffset = doc.getLineOffset(line - 1);
                    start = lineOffset + antlrError.getCharPositionInLine();
                    stop = start;
                }
                marker.setAttribute(IMarker.CHAR_START, start);
                marker.setAttribute(IMarker.CHAR_END, stop + 1);
            } catch (BadLocationException | CoreException ex) {
                Log.log(ex);
            }
        }
    }

    /**
     * Loads database schema from a ModelExporter directory tree.
     *
     * @return database schema
     */
    public static PgDatabase loadDatabaseSchemaFromIProject(IProject iProject,
            PgDiffArguments arguments, IProgressMonitor monitor,
            List<StatementBodyContainer> statementBodies, List<AntlrError> errors)
                    throws InterruptedException, IOException, CoreException {
        PgDatabase db = new PgDatabase(false);
        db.setArguments(arguments);
        for (WORK_DIR_NAMES workDirName : WORK_DIR_NAMES.values()) {
            IFolder iFolder = iProject.getFolder(workDirName.name());
            if (iFolder.exists()) {
                loadSubdir(iFolder, db, monitor, statementBodies, errors);
            }
        }

        IFolder schemasCommonDir = iProject.getFolder(WORK_DIR_NAMES.SCHEMA.name());
        // skip walking SCHEMA folder if it does not exist
        if (!schemasCommonDir.exists()) {
            return db;
        }

        // step 2
        // read out schemas names, and work in loop on each
        for (PgSchema schema : db.getSchemas()) {
            IFolder schemaFolder = schemasCommonDir.getFolder(ModelExporter.getExportedFilename(schema));
            for (String dirSub : DIR_LOAD_ORDER) {
                IFolder iFolder = schemaFolder.getFolder(dirSub);
                if (iFolder.exists()) {
                    loadSubdir(iFolder, db, monitor, statementBodies, errors);
                }
            }
        }
        return db;
    }

    private static void loadSubdir(IFolder folder, PgDatabase db, IProgressMonitor monitor,
            List<StatementBodyContainer> statementBodies, List<AntlrError> errors)
                    throws InterruptedException, IOException, CoreException {
        for (IResource resource : folder.members()) {
            if (resource.getType() == IResource.FILE && "sql".equals(resource.getFileExtension())) { //$NON-NLS-1$
                loadFile((IFile) resource, monitor, db, statementBodies, errors);
            }
        }
    }

    private static void loadFile(IFile file, IProgressMonitor monitor, PgDatabase db,
            List<StatementBodyContainer> statementBodies, List<AntlrError> errors)
                    throws IOException, CoreException, InterruptedException {
        PgDiffArguments arguments = new PgDiffArguments();
        arguments.setInCharsetName(file.getCharset());

        List<AntlrError> errList = null;
        try (PgUIDumpLoader loader = new PgUIDumpLoader(file, arguments, monitor)) {
            errList = loader.getErrors();
            loader.setLoadReferences(statementBodies != null);
            loader.loadFile(db);
            if (statementBodies != null) {
                statementBodies.addAll(loader.getStatementBodyReferences());
            }
        } finally {
            if (errors != null && errList != null && !errList.isEmpty()) {
                errors.addAll(errList);
            }
        }
    }

    public static PgStatement parseStatement(IFile file, Collection<DbObjType> types) throws InterruptedException, IOException, CoreException {
        return PgDatabase.listPgObjects(buildFiles(Arrays.asList(file), null, null))
                .values().stream().filter(e -> types.contains(e.getStatementType())).findAny().orElse(null);
    }

    public static PgDatabase buildFiles(Collection<IFile> files, IProgressMonitor monitor,
            List<StatementBodyContainer> statementBodies) throws InterruptedException, IOException, CoreException {
        SubMonitor mon = SubMonitor.convert(monitor, files.size());
        Set<String> schemaDirnamesLoaded = new HashSet<>();
        IPath schemasPath = new Path(WORK_DIR_NAMES.SCHEMA.name());
        PgDatabase db = new PgDatabase(false);
        db.setArguments(new PgDiffArguments());

        for (IFile file : files) {
            IPath filePath = file.getProjectRelativePath();
            if (!"sql".equals(file.getFileExtension()) || !isInProject(filePath)) { //$NON-NLS-1$
                // skip non-sql or non-project files
                // still report work
                mon.worked(1);
                continue;
            }

            if (schemasPath.isPrefixOf(filePath)) {
                IPath relSchemasPath = filePath.makeRelativeTo(schemasPath);
                String schemaDirname;
                boolean schemaDefSql = relSchemasPath.segmentCount() == 1;
                if (schemaDefSql) {
                    // schema definition SQL-file
                    schemaDirname = relSchemasPath.removeFileExtension().lastSegment();
                } else {
                    // schema-contained object
                    // preload its schema so that search_path parses normally
                    schemaDirname = relSchemasPath.segment(0);
                }
                if (!schemaDirnamesLoaded.add(schemaDirname)) {
                    // schema already loaded, skip
                    if (schemaDefSql) {
                        // report schema pre-built if the same schema was to be built normally as well
                        mon.worked(1);
                        continue;
                    }
                } else if (!schemaDefSql) {
                    // pre-load schema for object's search path
                    // otherwise we're dealing with the schema file itself, allow it to load normally
                    // don't pass progress monitor since this file isn't in the original load-set
                    loadFile(file.getProject().getFile(schemasPath.append(schemaDirname + ".sql")), //$NON-NLS-1$
                            null, db, statementBodies, null);
                }
            }

            loadFile(file, mon, db, statementBodies, null);
        }
        return db;
    }

    public static int countFiles(IContainer container) throws CoreException {
        int[] count = new int[1];
        container.accept(p -> {
            if (p.getType() == IResource.FILE) {
                ++count[0];
            }
            return true;
        }, 0);
        return count[0];
    }

    /**
     * @param path project relative path of checked resource
     * @return whether this resource is within the main database schema hierarchy
     */
    public static boolean isInProject(IPath path) {
        String dir = path.segment(0);
        return dir == null ? false : Arrays.stream(ApgdiffConsts.WORK_DIR_NAMES.values())
                .map(Enum::name).anyMatch(dir::equals);
    }

    public static boolean isInProject(IResource resource) {
        try {
            return resource.getProject().hasNature(NATURE.ID)
                    && isInProject(resource.getProjectRelativePath());
        } catch (CoreException ex) {
            Log.log(ex);
            return false;
        }
    }

    public static boolean isInProject(IResourceDelta delta) {
        return isInProject(delta.getProjectRelativePath());
    }

    public static boolean isInProject(IEditorInput editorInput) {
        IResource res = ResourceUtil.getResource(editorInput);
        return res == null ? false : isInProject(res);
    }

    /**
     * @param editorInput
     * @return param's {@link IResource} or null if not available or not {@link #isInProject(IPath)}
     */
    public static IResource getProjectResource(IEditorInput editorInput) {
        IResource res = ResourceUtil.getResource(editorInput);
        return isInProject(res) ? res : null;
    }

    /**
     * @param path project relative path
     * @return whether the path corresponds to a schema sql file
     *          like this: /SCHEMA/schema_name.sql
     */
    public static boolean isSchemaFile(IPath path) {
        return path.segmentCount() == 2 && path.segment(0).equals(WORK_DIR_NAMES.SCHEMA.name())
                && path.segment(1).endsWith(".sql"); //$NON-NLS-1$
    }
}
