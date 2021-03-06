package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.List;

import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Alter_view_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.VexContext;
import cz.startnet.utils.pgdiff.parsers.antlr.expr.ValueExpr;
import cz.startnet.utils.pgdiff.parsers.antlr.rulectx.Vex;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgView;

public class AlterView extends ParserAbstract {
    private final Alter_view_statementContext ctx;
    public AlterView(Alter_view_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public PgStatement getObject() {
        List<IdentifierContext> ids = ctx.name.identifier();
        PgSchema schema = getSchemaSafe(ids, db.getDefaultSchema());
        PgView dbView = getSafe(schema::getView, QNameParser.getFirstNameCtx(ids));
        fillOwnerTo(ctx.owner_to(), dbView);
        if (ctx.set_def_column() != null) {
            VexContext exp = ctx.set_def_column().expression;
            ValueExpr vex = new ValueExpr(schema.getName());
            vex.analyze(new Vex(exp));
            dbView.addAllDeps(vex.getDepcies());
            dbView.addColumnDefaultValue(getFullCtxText(ctx.column_name), getFullCtxText(exp));
        }
        if (ctx.drop_def() != null) {
            dbView.removeColumnDefaultValue(getFullCtxText(ctx.column_name));
        }
        return null;
    }
}