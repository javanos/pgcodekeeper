package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.nio.file.Path;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Alter_function_statementContext;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgStatement;

public class AlterFunction extends ParserAbstract {

    private Alter_function_statementContext ctx;
    public AlterFunction(Alter_function_statementContext ctx, PgDatabase db, Path filePath) {
        super(db, filePath);
        this.ctx = ctx;
    }

    @Override
    public PgStatement getObject() {
        String name = getName(ctx.function_parameters().name);
        String schemaName = getSchemaName(ctx.function_parameters().name);
        if (schemaName == null) {
            schemaName = getDefSchemaName();
        }
        PgFunction function = new PgFunction(name, getFullCtxText(ctx.getParent()));
        fillArguments(ctx.function_parameters().function_args(), function, getDefSchemaName());
        PgFunction func= db.getSchema(schemaName).getFunction(function.getSignature());
        if (func == null) {
            logError("FUNCTION", name);
            return null;
        }
        fillOwnerTo(ctx.owner_to(), func);
        return null;
    }

}