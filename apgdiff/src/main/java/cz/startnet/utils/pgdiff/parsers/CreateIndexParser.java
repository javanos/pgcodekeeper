/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.parsers;

import java.text.MessageFormat;

import ru.taximaxim.codekeeper.apgdiff.localizations.Messages;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgIndex;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgTable;

/**
 * Parses CREATE INDEX statements.
 *
 * @author fordfrog
 */
public final class CreateIndexParser {

    /**
     * Parses CREATE INDEX statement.
     *
     * @param database  database
     * @param statement CREATE INDEX statement
     */
    public static void parse(final PgDatabase database,
            final String statement, final String searchPath) {
        final Parser parser = new Parser(statement);
        parser.expect("CREATE");

        final boolean unique = parser.expectOptional("UNIQUE");

        parser.expect("INDEX");
        parser.expectOptional("CONCURRENTLY");

        final String indexName =
                ParserUtils.getObjectName(parser.parseIdentifier());

        parser.expect("ON");

        final String tableName = parser.parseIdentifier();
        final String definition = parser.getRest();
        final String schemaName =
                ParserUtils.getSchemaName(tableName, database);
        final PgSchema schema = database.getSchema(schemaName);

        if (schema == null) {
            throw new ParserException(MessageFormat.format(
                    Messages.Parser_CannotFindSchema, schemaName,
                    statement));
        }

        final String objectName = ParserUtils.getObjectName(tableName);
        final PgTable table = schema.getTable(objectName);

        if (table == null) {
            throw new ParserException(MessageFormat.format(
                    Messages.Parser_CannotFindTable, tableName,
                    statement));
        }

        final PgIndex index = new PgIndex(indexName, statement);
        table.addIndex(index);
        index.setDefinition(definition.trim());
        index.setTableName(table.getName());
        index.setUnique(unique);
    }

    /**
     * Creates a new instance of CreateIndexParser.
     */
    private CreateIndexParser() {
    }
}