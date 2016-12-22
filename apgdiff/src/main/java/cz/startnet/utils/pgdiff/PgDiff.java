/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.loader.JdbcLoader;
import cz.startnet.utils.pgdiff.loader.PgDumpLoader;
import cz.startnet.utils.pgdiff.schema.PgColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgTable;
import ru.taximaxim.codekeeper.apgdiff.ignoreparser.IgnoreParser;
import ru.taximaxim.codekeeper.apgdiff.licensing.LicenseException;
import ru.taximaxim.codekeeper.apgdiff.localizations.Messages;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.CompareTree;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DiffTree;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.IgnoreList;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeFlattener;
import ru.taximaxim.codekeeper.apgdiff.model.graph.ActionsToScriptConverter;
import ru.taximaxim.codekeeper.apgdiff.model.graph.DepcyResolver;

/**
 * Creates diff of two database schemas.
 *
 * @author fordfrog
 */
public final class PgDiff {

    /**
     * Creates diff on the two database schemas.
     *
     * @param writer    writer the output should be written to
     * @param arguments object containing arguments settings
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public static PgDiffScript createDiff(PrintWriter writer, PgDiffArguments arguments)
            throws InterruptedException, IOException, LicenseException, URISyntaxException {
        PgDatabase oldDatabase = loadDatabaseSchema(
                arguments.getOldSrcFormat(), arguments.getOldSrc(), arguments);
        PgDatabase newDatabase = loadDatabaseSchema(
                arguments.getNewSrcFormat(), arguments.getNewSrc(), arguments);

        IgnoreParser ignoreParser = new IgnoreParser();
        for (String listFilename : arguments.getIgnoreLists()) {
            ignoreParser.parse(Paths.get(listFilename));
        }

        return diffDatabaseSchemas(writer, arguments, oldDatabase, newDatabase, ignoreParser.getIgnoreList());
    }

    /**
     * Loads database schema choosing the provided method.
     *
     * @param format        format of the database source, must be "dump", "parsed" or "db"
     *                         otherwise exception is thrown
     * @param srcPath        path to the database source to load
     * @param arguments        object containing arguments settings
     *
     * @return the loaded database
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    static PgDatabase loadDatabaseSchema(String format, String srcPath,
            PgDiffArguments arguments) throws InterruptedException, IOException, LicenseException, URISyntaxException {
        if(format.equals("dump")) {
            try (PgDumpLoader loader = new PgDumpLoader(new File(srcPath), arguments)) {
                return loader.load();
            }
        } else if(format.equals("parsed")) {
            return PgDumpLoader.loadDatabaseSchemaFromDirTree(srcPath,
                    arguments, null, null);
        } else if(format.equals("db")) {
            JdbcLoader loader = new JdbcLoader(new JdbcConnector(srcPath), arguments);
            return loader.getDbFromJdbc();
        }

        throw new UnsupportedOperationException(
                MessageFormat.format(Messages.UnknownDBFormat, format));
    }

    /**
     * Creates diff from comparison of two database schemas.<br><br>
     * Following PgDiffArguments methods are called from this method:<br>
     * isAddTransaction()<br>
     * isOutputIgnoredStatements()<br>
     * isIgnoreStartWith()<br>
     * isAddDefaults()<br>
     * isIgnoreFunctionWhitespace()<br>
     *
     * @param writer      writer the output should be written to
     * @param arguments   object containing arguments settings
     * @param oldDatabase original database schema
     * @param newDatabase new database schema
     * @throws InterruptedException
     */
    public static PgDiffScript diffDatabaseSchemas(PrintWriter writer,
            PgDiffArguments arguments, PgDatabase oldDbFull, PgDatabase newDbFull,
            IgnoreList ignoreList) throws InterruptedException {
        TreeElement root = DiffTree.create(oldDbFull, newDbFull, null);
        root.setAllChecked();
        return diffDatabaseSchemasAdditionalDepcies(writer, arguments,
                root, oldDbFull, newDbFull, null, null, ignoreList);
    }

    /**
     * Делает то же, что и метод выше, однако принимает TreeElement - как
     * элементы нужные для наката
     */
    public static PgDiffScript diffDatabaseSchemasAdditionalDepcies(PrintWriter writer,
            PgDiffArguments arguments, TreeElement root,
            PgDatabase oldDbFull, PgDatabase newDbFull,
            List<Entry<PgStatement, PgStatement>> additionalDepciesSource,
            List<Entry<PgStatement, PgStatement>> additionalDepciesTarget) {
        return diffDatabaseSchemasAdditionalDepcies(writer, arguments, root,
                oldDbFull, newDbFull, additionalDepciesSource, additionalDepciesTarget, null);
    }

    private static PgDiffScript diffDatabaseSchemasAdditionalDepcies(PrintWriter writer,
            PgDiffArguments arguments, TreeElement root,
            PgDatabase oldDbFull, PgDatabase newDbFull,
            List<Entry<PgStatement, PgStatement>> additionalDepciesSource,
            List<Entry<PgStatement, PgStatement>> additionalDepciesTarget,
            IgnoreList ignoreList) {
        PgDiffScript script = new PgDiffScript();

        if (arguments.getTimeZone() != null) {
            script.addStatement("SET TIMEZONE TO "
                    + PgDiffUtils.quoteString(arguments.getTimeZone()) + ';');
        }

        if (!arguments.isCheckFunctionBodies()) {
            script.addStatement("SET check_function_bodies = false;");
        }

        if (arguments.isAddTransaction()) {
            script.addStatement("START TRANSACTION;");
        }

        DepcyResolver depRes = new DepcyResolver(oldDbFull, newDbFull);
        if (additionalDepciesSource != null) {
            depRes.addCustomDepciesToOld(additionalDepciesSource);
        }
        if (additionalDepciesTarget != null) {
            depRes.addCustomDepciesToNew(additionalDepciesTarget);
        }

        // TODO when live DB connection is impelemted, pass the DB names to IgnoreList
        List<TreeElement> selected = new TreeFlattener()
                .onlySelected()
                .useIgnoreList(ignoreList)
                .flatten(root);
        //TODO----------КОСТЫЛЬ колонки добавляются как выбранные если выбрана таблица-----------
        addColumnsAsElements(oldDbFull, newDbFull, selected);
        // ---КОСТЫЛЬ-----------

        Collections.sort(selected, new CompareTree());
        for (TreeElement st : selected) {
            switch (st.getSide()) {
            case LEFT:
                depRes.addDropStatements(st.getPgStatement(oldDbFull));
                break;
            case BOTH:
                depRes.addAlterStatements(st.getPgStatement(oldDbFull),
                        st.getPgStatement(newDbFull));
                break;
            case RIGHT:
                depRes.addCreateStatements(st.getPgStatement(newDbFull));
                break;
            }
        }
        depRes.recreateDrops();

        new ActionsToScriptConverter(depRes.getActions()).fillScript(script);
        if (arguments.isAddTransaction()) {
            script.addStatement("COMMIT TRANSACTION;");
        }

        script.printStatements(writer);
        if (arguments.isOutputIgnoredStatements()) {
            addIgnoredStatements(oldDbFull, Messages.Database_OriginalDatabaseIgnoredStatements, writer);
            addIgnoredStatements(newDbFull, Messages.Database_NewDatabaseIgnoredStatements, writer);
        }
        return script;
    }

    /**
     * После реализации колонок как подэлементов таблицы выпилить метод!
     */
    @Deprecated
    private static void addColumnsAsElements(PgDatabase oldDbFull, PgDatabase newDbFull,
            List<TreeElement> selected) {
        List<TreeElement> tempColumns = new ArrayList<>();
        for (TreeElement el : selected) {
            if (el.getType() == DbObjType.TABLE && el.getSide() == DiffSide.BOTH) {
                PgTable oldTbl =(PgTable) el.getPgStatement(oldDbFull);
                PgTable newTbl =(PgTable) el.getPgStatement(newDbFull);
                for (PgColumn oldCol : oldTbl.getColumns()) {
                    PgColumn newCol = newTbl.getColumn(oldCol.getName());
                    if (newCol == null) {
                        TreeElement col = new TreeElement(oldCol.getName(), DbObjType.COLUMN, DiffSide.LEFT);
                        col.setParent(el);
                        tempColumns.add(col);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        AtomicBoolean isNeedDepcies = new AtomicBoolean();
                        if (oldCol.appendAlterSQL(newCol, sb, isNeedDepcies)) {
                            TreeElement col = new TreeElement(oldCol.getName(), DbObjType.COLUMN, DiffSide.BOTH);
                            col.setParent(el);
                            tempColumns.add(col);
                        }
                    }
                }
                for (PgColumn newCol : newTbl.getColumns()) {
                    if (!oldTbl.containsColumn(newCol.getName())) {
                        TreeElement col = new TreeElement(newCol.getName(), DbObjType.COLUMN, DiffSide.RIGHT);
                        col.setParent(el);
                        tempColumns.add(col);
                    }
                }
            }
        }
        selected.addAll(tempColumns);
    }

    /**
     * Adds ignored Statements to script
     * @param database database with ignored statements
     * @param messageText resource for localization message
     * @param script script to output statements
     */
    private static void addIgnoredStatements(PgDatabase database,
            String messageText, PrintWriter writer) {
        if (!database.getIgnoredStatements().isEmpty()) {
            writer.println();
            writer.print("/*");
            writer.println(messageText);

            for (final String statement : database.getIgnoredStatements()) {
                writer.println();
                writer.println(statement);
            }
            writer.println("*/");
        }
    }

    private PgDiff() {
    }
}
