package cz.startnet.utils.pgdiff.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.parsers.antlr.exception.ObjectCreationException;
import ru.taximaxim.codekeeper.apgdiff.Log;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

/**
 * The superclass for general pgsql statement.
 * All changes to hashed fields of extending classes must be
 * followed by a {@link #resetHash()} call.
 *
 * @author Alexander Levsha
 */
public abstract class PgStatement implements IStatement {
    /**
     * The statement as it's been read from dump before parsing.
     */
    private final String rawStatement;
    protected final String name;
    protected String owner;
    protected String comment;
    protected final Set<PgPrivilege> grants = new LinkedHashSet<>();
    protected final Set<PgPrivilege> revokes = new LinkedHashSet<>();

    private PgStatement parent;
    protected final List<GenericColumn> deps = new ArrayList<>();

    private String location;
    private boolean isLib;

    // 0 means not calculated yet and/or hash has been reset
    private int hash;

    public PgStatement(String name, String rawStatement) {
        this.name = name;
        this.rawStatement = rawStatement;
    }

    public String getRawStatement() {
        return rawStatement;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * @return Always returns just the object's name.
     */
    public final String getBareName() {
        return name;
    }

    @Override
    public abstract DbObjType getStatementType();

    @Override
    public PgStatement getParent() {
        return parent;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isLib() {
        return isLib;
    }

    public void markAsLib() {
        this.isLib = true;
    }

    public abstract PgDatabase getDatabase();

    public void dropParent() {
        parent = null;
    }

    public void setParent(PgStatement parent) {
        if(this.parent != null) {
            throw new IllegalStateException("Statement already has a parent: "
                    + this.getClass() + " Name: " + this.getName());
        }

        this.parent = parent;
    }

    public List<GenericColumn> getDeps() {
        return Collections.unmodifiableList(deps);
    }

    public void addDep(GenericColumn dep){
        // TODO remove after fix
        if (dep == null) {
            Log.log(new Exception("null dependency added for " + getQualifiedName()));
        }
        deps.add(dep);
    }

    public void addAllDeps(Collection<GenericColumn> deps){
        // TODO remove after fix
        if (deps.contains(null)) {
            Log.log(new Exception("null dependency added for " + getQualifiedName()));
        }
        this.deps.addAll(deps);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        resetHash();
    }

    /**
     * Sets {@link #comment} with newlines as requested in arguments.
     */
    public void setComment(PgDiffArguments args, String comment) {
        setComment(args.isKeepNewlines() ? comment : comment.replace("\r", ""));
    }

    protected StringBuilder appendCommentSql(StringBuilder sb) {
        sb.append("COMMENT ON ");
        DbObjType type = getStatementType();
        sb.append(type).append(' ');
        switch (type) {
        case COLUMN:
            sb.append(PgDiffUtils.getQuotedName(getParent().getName()))
            .append('.')
            .append(PgDiffUtils.getQuotedName(getName()));
            break;
        case FUNCTION:
            ((PgFunction) this).appendFunctionSignature(sb, false, true);
            break;

        case CONSTRAINT:
        case TRIGGER:
        case RULE:
            sb.append(PgDiffUtils.getQuotedName(getName()))
            .append(" ON ")
            .append(PgDiffUtils.getQuotedName(getParent().getName()));
            break;

        case DATABASE:
            sb.append("current_database()");
            break;

        default:
            sb.append(PgDiffUtils.getQuotedName(getName()));
        }

        return sb.append(" IS ")
                .append(comment == null || comment.isEmpty() ? "NULL" : comment)
                .append(';');
    }

    public String getCommentSql() {
        return appendCommentSql(new StringBuilder()).toString();
    }

    public Set<PgPrivilege> getGrants() {
        return Collections.unmodifiableSet(grants);
    }

    public Set<PgPrivilege> getRevokes() {
        return Collections.unmodifiableSet(revokes);
    }

    public void addPrivilege(PgPrivilege privilege) {
        if (privilege.isRevoke()) {
            revokes.add(privilege);
        } else {
            grants.add(privilege);
        }
        privilege.setParent(this);
        resetHash();
    }

    protected StringBuilder appendPrivileges(StringBuilder sb) {
        if (grants.isEmpty() && revokes.isEmpty()) {
            return sb;
        }

        sb.append("\n\n-- ")
        .append(getStatementType())
        .append(' ')
        .append(getName())
        .append(' ')
        .append("GRANT\n");

        for (PgPrivilege priv : revokes) {
            sb.append('\n').append(priv.getCreationSQL());
        }
        for (PgPrivilege priv : grants) {
            sb.append('\n').append(priv.getCreationSQL());
        }

        return sb;
    }

    public String getPrivilegesSQL() {
        return appendPrivileges(new StringBuilder()).toString();
    }

    protected void alterPrivileges(PgStatement newObj, StringBuilder sb){
        // first drop (revoke) missing grants
        boolean grantsChanged = false;
        Set<PgPrivilege> newGrants = newObj.getGrants();
        for (PgPrivilege grant : grants) {
            if (!newGrants.contains(grant)) {
                grantsChanged = true;
                sb.append('\n').append(grant.getDropSQL());
            }
        }

        // now set all privileges if there are any changes
        grantsChanged = grantsChanged || grants.size() != newGrants.size();
        if (grantsChanged || !revokes.equals(newObj.getRevokes())) {
            newObj.appendPrivileges(sb);
            if (newObj.revokes.isEmpty() && newObj.grants.isEmpty()) {
                PgPrivilege.appendDefaultPrivileges(newObj, sb);
            }
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        resetHash();
    }

    protected StringBuilder appendOwnerSQL(StringBuilder sb) {
        if (owner == null) {
            return sb;
        }

        DbObjType type = getStatementType();
        sb.append("\n\nALTER ")
        .append(type)
        .append(' ');
        if (type == DbObjType.FUNCTION) {
            ((PgFunction) this).appendFunctionSignature(sb, false, true);
        } else {
            sb.append(PgDiffUtils.getQuotedName(getName()));
        }
        sb.append(" OWNER TO ")
        .append(PgDiffUtils.getQuotedName(owner))
        .append(';');

        return sb;
    }

    public String getOwnerSQL() {
        return appendOwnerSQL(new StringBuilder()).toString();
    }

    public abstract String getCreationSQL();

    public String getFullSQL() {
        return getCreationSQL();
    }

    public abstract String getDropSQL();

    /**
     * Метод заполняет sb выражением изменения объекта, можно ли изменить объект
     * ALTER.<br><br>
     *
     * Результат работы метода определяется по паре значений:
     * возвращаемое значение и длина sb.length().<br>
     * Возвращаемое значение говорит о статусе объекта: изменился или нет.<br>
     * sb.length() говорит о возможностиизменить состояние объекта ALTERом
     * (если объект вообще изменился).<br><br>
     *
     * <code>sb == 0 && rv == false</code> - не требуется действий<br>
     * <code>sb >  0 && rv == false</code> - illegal state, неизмененный объект с ALTER<br>
     * <code>sb == 0 && rv == true</code>  - ALTER невозможен, делаем DROP/CREATE<br>
     * <code>sb >  0 && rv == true</code>  - делаем ALTER
     *
     * @param newCondition новое состоятние объекта
     * @param sb скрипт изменения
     * @param isNeedDepcies out параметр: нужно ли использовать зависимости объекта
     * @return true - необходимо изменить объект, используя DROP в случае
     *                 невозможности ALTER, false - объект не изменился
     */
    public abstract boolean appendAlterSQL(PgStatement newCondition, StringBuilder sb,
            AtomicBoolean isNeedDepcies);

    /**
     * Copies all object properties into a new object and leaves all its children empty.
     *
     * @return shallow copy of a DB object.
     */
    public abstract PgStatement shallowCopy();

    /**
     * Performs {@link #shallowCopy()} on this object and all its children.
     *
     * @return a fully recursive copy of this statement.
     */
    public abstract PgStatement deepCopy();

    /**
     * This method does not account for nested child PgStatements.
     * Shallow version of {@link #equals(Object)}
     */
    public abstract boolean compare(PgStatement obj);

    /**
     * Deep part of {@link #equals(Object)}.
     * Compares all object's child PgStatements for equality.
     */
    public boolean compareChildren(PgStatement obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Null PgStatement!");
        }
        return true;
    }

    /**
     * Compares this object and all its children with another statement.
     * <hr><br>
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj){
        if (this == obj) {
            return true;
        } else if (obj instanceof PgStatement) {
            PgStatement st = (PgStatement) obj;
            return this.parentNamesEquals(st)
                    && this.compare(st)
                    && this.compareChildren(st);
        }
        return false;
    }

    /**
     * Recursively compares objects' parents
     * to ensure their equal position in their object trees.
     */
    private boolean parentNamesEquals(PgStatement st){
        PgStatement p = parent;
        PgStatement p2 = st.getParent();
        while (p != null && p2 != null){
            if (!Objects.equals(p.getName(), p2.getName())){
                return false;
            }
            p = p.getParent();
            p2 = p2.getParent();
        }
        return p == null && p2 == null;
    }

    /**
     * Calls {@link #computeHash()}. Modifies that value with combined hashcode
     * of all parents of this object in the tree to complement
     * {@link #parentNamesEquals(PgStatement)} and {@link #equals(Object)}<br>
     * Caches the hashcode value until recalculation is requested via {@link #resetHash()}.
     * Always request recalculation when you change the hashed fields.<br>
     * Do actual hashing in {@link #computeHash()}.
     * <hr><br>
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        int h = hash;
        if (h == 0) {
            h = computeHash();

            final int prime = 31;
            PgStatement p = parent;
            while (p != null) {
                String pName = p.getName();
                h = prime * h + ((pName == null) ? 0 : pName.hashCode());
                p = p.getParent();
            }

            if (h == 0) {
                h = Integer.MAX_VALUE;
            }
            hash = h;
        }
        return h;
    }

    protected void resetHash(){
        PgStatement st = this;
        while (st != null){
            st.hash = 0;
            st = st.getParent();
        }
    }

    /**
     * @see #hashCode()
     */
    public abstract int computeHash();

    /**
     * @return fully qualified (up to schema) dot-delimited object name.
     *          Identifiers are quoted.
     */
    public String getQualifiedName() {
        String qname = PgDiffUtils.getQuotedName(getName());

        PgStatement par = this.parent;
        while (par != null) {
            if (par instanceof PgDatabase) {
                break;
            }
            qname = PgDiffUtils.getQuotedName(par.getName())
                    + '.' + qname;
            par = par.getParent();
        }

        return qname;
    }

    @Override
    public String toString() {
        return name == null ? "Unnamed object" : name;
    }

    protected void assertUnique(Function<String, ? extends PgStatement> getter,
            PgStatement newSt) {
        PgStatement found = getter.apply(newSt.getName());
        if (found != null) {
            PgStatement foundParent = found.getParent();
            throw foundParent instanceof PgStatementWithSearchPath
            ? new ObjectCreationException(newSt, foundParent)
                    : new ObjectCreationException(newSt);
        }
    }
}
