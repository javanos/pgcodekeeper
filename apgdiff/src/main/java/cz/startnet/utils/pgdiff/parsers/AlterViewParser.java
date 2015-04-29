/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.parsers;

import java.text.MessageFormat;

import ru.taximaxim.codekeeper.apgdiff.localizations.Messages;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgView;

/**
 * Parses ALTER VIEW statements.
 *
 * @author fordfrog
 */
public final class AlterViewParser {

    /**
     * Parses ALTER VIEW statement.
     *
     * @param database                database
     * @param statement               ALTER VIEW statement
     * @param outputIgnoredStatements whether ignored statements should be
     *                                output in the diff
     */
    public static void parse(final PgDatabase database,
            final String statement, final boolean outputIgnoredStatements) {
        final Parser parser = new Parser(statement);
        parser.expect("ALTER", "VIEW");

        final String viewName = parser.parseIdentifier();
        final String schemaName = ParserUtils.getSchemaName(viewName, database);
        final PgSchema schema = database.getSchema(schemaName);

        if (schema == null) {
            throw new ParserException(MessageFormat.format(
                    Messages.Parser_CannotFindSchema, schemaName,
                    statement));
        }

        final String objectName = ParserUtils.getObjectName(viewName);
        final PgView view = schema.getView(objectName);

        if (view == null) {
            throw new ParserException(MessageFormat.format(
                    Messages.Parser_CannotFindView, viewName,
                    statement));
        }

        while (!parser.expectOptional(";")) {
            if (parser.expectOptional("ALTER")) {
                parser.expectOptional("COLUMN");

                final String columnName =
                        ParserUtils.getObjectName(parser.parseIdentifier());

                if (parser.expectOptional("SET", "DEFAULT")) {
                    final String expression = parser.getExpression();
                    view.addColumnDefaultValue(columnName, expression);
                } else if (parser.expectOptional("DROP", "DEFAULT")) {
                    view.removeColumnDefaultValue(columnName);
                } else {
                    parser.throwUnsupportedCommand();
                }
            } else if (parser.expectOptional("OWNER", "TO")) {
                view.setOwner(parser.parseIdentifier());
            } else {
                parser.throwUnsupportedCommand();
            }
        }
    }

    /**
     * Creates new instance of AlterViewParser.
     */
    private AlterViewParser() {
    }
}