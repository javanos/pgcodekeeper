package cz.startnet.utils.pgdiff.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class PgSelect extends PgStatement {
    
    private final List<GenericColumn> columns = new ArrayList<>();
    
    public PgSelect(String rawStatement) {
        super(null, rawStatement);
    }
    
    @Override
    public DbObjType getStatementType() {
        return null;
    }
    
    /**
     * Adds a column to the column list if it doesn't contain the same column.
     */
    public void addColumn(GenericColumn column) {
        if (!columns.contains(column)) {
            columns.add(column);
            resetHash();
        }
    }
    
    /**
     * @return unmodifiable copy of columns
     */
    public List<GenericColumn> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override
    public String getCreationSQL() {
        return null;
    }
    
    @Override
    public String getDropSQL() {
        return null;
    }
    
    @Override
    public boolean appendAlterSQL(PgStatement newCondition, StringBuilder sb, AtomicBoolean isNeedDepcies) {
        return false;
    }

    @Override
    public PgSelect shallowCopy() {
        PgSelect selectDst = new PgSelect(getRawStatement());
        selectDst.columns.addAll(columns);
        return selectDst;
    }

    @Override
    public PgSelect deepCopy() {
        return shallowCopy();
    }

    @Override
    public boolean compare(PgStatement obj) {
        boolean eq = false;
        
        if (this == obj) {
            eq = true;
        } else if (obj instanceof PgSelect) {
            PgSelect select = (PgSelect) obj;
            eq = Objects.equals(columns, select.columns);
        }
        
        return eq;
    }

    @Override
    public int computeHash() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((columns == null) ? 0 : columns.hashCode());
        return result;
    }
}