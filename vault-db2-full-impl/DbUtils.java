/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
// "I don't think there's any more room for not considering underestimating the importance of beginning to start the
// process of mulling over the conceptualization of starting to worry. And the time to do it is... Very soon."

package org.jfrog.storage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jfrog.storage.DbType;
import org.jfrog.storage.JdbcHelper;
import org.jfrog.storage.util.functional.IOSQLThrowingConsumer;
import org.jfrog.storage.util.functional.SQLThrowingConsumer;
import org.jfrog.storage.util.functional.SQLThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * A utility class for common JDBC operations.
 *
 * @author Yossi Shaul
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class DbUtils {
    private static final Logger log = LoggerFactory.getLogger(DbUtils.class);

    public static final String VALUES = " VALUES";
    static final String COLUMN_SIZE = "COLUMN_SIZE";

    /**
     * Closes the given resources. Exceptions are just logged.
     *
     * @param con  The {@link Connection} to close
     * @param stmt The {@link Statement} to close
     * @param rs   The {@link ResultSet} to close
     */
    public static void close(@Nullable Connection con, @Nullable Statement stmt, @Nullable ResultSet rs) {
        try {
            close(rs);
        } finally {
            try {
                DbStatementUtils.close(stmt);
            } finally {
                close(con);
            }
        }
    }

    /**
     * Closes the given resources. Exceptions are just logged.
     *
     * @param con  The {@link Connection} to close
     * @param stmt The {@link Statement} to close
     * @param rs   The {@link ResultSet} to close
     */
    public static void close(@Nullable Connection con, @Nullable Statement stmt, @Nullable ResultSet rs,
            @Nullable DataSource ds) {
        try {
            close(rs);
        } finally {
            try {
                DbStatementUtils.close(stmt);
            } finally {
                close(con, ds);
            }
        }
    }

    /**
     * Closes the given connection and just logs any exception.
     *
     * @param con The {@link Connection} to close.
     */
    public static void close(@Nullable Connection con) {
        close(con, null);
    }

    /**
     * Closes the given connection and just logs any exception.
     *
     * @param con The {@link Connection} to close.
     */
    public static void close(@Nullable Connection con, @Nullable DataSource ds) {
        if (con != null) {
            try {
                DataSourceUtils.doReleaseConnection(con, ds);
            } catch (SQLException e) {
                log.trace("Could not close JDBC connection", e);
            } catch (Exception e) {
                log.trace("Unexpected exception when closing JDBC connection", e);
            }
        }
    }

    /**
     * Closes the given statement and just logs any exception.
     *
     * @param stmt The {@link Statement} to close.
     */
    public static void close(@Nullable Statement stmt) {
        DbStatementUtils.close(stmt);
    }

    /**
     * Closes the given result set and just logs any exception.
     *
     * @param rs The {@link ResultSet} to close.
     */
    public static void close(@Nullable ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.trace("Could not close JDBC result set", e);
            } catch (Exception e) {
                log.trace("Unexpected exception when closing JDBC result set", e);
            }
        }
    }

    public static void executeSqlStream(Connection con, InputStream in) throws IOException, SQLException {
        DbStatementUtils.executeSqlStream(con, in);
    }

    public static void closeDataSource(DataSource dataSource) {
        if (dataSource == null) {
            return;
        }
        if (dataSource instanceof Closeable) {
            try {
                ((Closeable) dataSource).close();
            } catch (Exception e) {
                String msg = "Error closing the data source " + dataSource + " due to:" + e.getMessage();
                if (log.isDebugEnabled()) {
                    log.error(msg, e);
                } else {
                    log.error(msg);
                }
            }
        }
    }

    /**
     * Catalog is NOT schema, to get a 1-1 match of any searchable object (table, column, index, keys, etc.) you should
     * use both, where it is applicable.
     *
     * Oracle -> Schema only, based on current logged in user.
     * Derby -> Schema only, retrievable by jdbc metadata
     * Mysql + Maria -> No schema, catalog is the active database user logged in to
     * Postgres -> Schema (usually 'public') and catalog is active database user is logged in to
     * MSSQL -> Schema (usually 'dbo') and catalog is active database user is logged in to (i.e. USING in ms dialect)
     */
    public static String getActiveSchema(Connection conn, DbType dbType) throws SQLException {
        String schema;
        switch (dbType) {
	    case DB2:
	    case MYSQL:
            case MARIADB:
            case POSTGRESQL:
            case DERBY:
                schema = conn.getSchema();
                break;
            case ORACLE:
                // getUserName should be enough, if issues arise, use this instead:
                //SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL
                schema = conn.getMetaData().getUserName();
                break;
            case MSSQL:
                //MS does not support setting schema on active connection, so it makes no difference.
                //The driver i'm testing with actually throws AbstractMethodErrorException - seems its fixed later on
                //https://docs.microsoft.com/en-us/sql/connect/jdbc/jdbc-4-1-compliance-for-the-jdbc-driver?view=sql-server-2017
                schema = null;
                break;
            default:
                throw new IllegalArgumentException("Unrecognized db type: " + dbType.name());
        }
        if (log.isDebugEnabled()) {
            log.debug("Active schema resolved to: {}, DB type: {}", schema, dbType.name());
        }
        return schema;
    }

    /**
     * Catalog is NOT schema, to get a 1-1 match of any searchable object (table, column, index, keys, etc.) you should
     * use both, where it is applicable.
     *
     * Oracle -> Schema only, based on current logged in user.
     * Derby -> Schema only, retrievable by jdbc metadata
     * Mysql + Maria -> No schema, catalog is the active database user logged in to
     * Postgres -> Schema (usually 'public') and catalog is active database user is logged in to
     * MSSQL -> Schema (usually 'dbo') and catalog is active database user is logged in to (i.e. USING in ms dialect)
     */
    public static String getActiveCatalog(Connection conn, DbType dbType) {
        String catalog = null;
        try {
            switch (dbType) {
                case DERBY:
                case ORACLE:
                    //No catalog in derby and oracle.
                    break;
		case DB2:
                case MYSQL:
                case MARIADB:
                case POSTGRESQL:
                case MSSQL:
                    catalog = conn.getCatalog();
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized db type: " + dbType.name());
            }
        } catch (Exception e) {
            log.error("Can't obtain active catalog from db: ", e);
        }
        log.debug("Current active catalog: {}", catalog);
        return catalog;
    }

    public static String normalizedName(String name, DatabaseMetaData metaData) throws SQLException {
        if (metaData.storesLowerCaseIdentifiers()) {
            name = name.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            name = name.toUpperCase();
        }
        return name;
    }

    public static boolean tableExists(JdbcHelper jdbcHelper, DbType dbType, String tableName) throws SQLException {
        return withMetadata(jdbcHelper, metadata -> tableExists(metadata, dbType, tableName));
    }

    public static boolean tableExists(DatabaseMetaData metadata, DbType dbType, String tableName) throws SQLException {
        tableName = normalizedName(tableName, metadata);
        String activeCatalog = getActiveCatalog(metadata.getConnection(), dbType);
        String activeSchema = getActiveSchema(metadata.getConnection(), dbType);
        log.debug("Searching for table '{}' under schema '{}' catalog '{}'", tableName, activeSchema, activeCatalog);
        try (ResultSet rs = metadata.getTables(activeCatalog, activeSchema, tableName, new String[]{"TABLE"})) {
            boolean hasNext = rs.next();
            if (hasNext) {
                String table = rs.getString(1);
                log.debug("Searched for table: '{}'. Got result: '{}'", tableName, table);
            }
            return hasNext;
        }
    }

    public static boolean columnExists(JdbcHelper jdbcHelper, DbType dbType, String tableName, String columnName)
            throws SQLException {
        return withMetadata(jdbcHelper, metadata -> columnExists(metadata, dbType, tableName, columnName));
    }

    public static boolean columnExists(DatabaseMetaData metadata, DbType dbType, String tableName, String columnName)
            throws SQLException {
        columnName = normalizedName(columnName, metadata);
        tableName = normalizedName(tableName, metadata);
        String activeSchema = getActiveSchema(metadata.getConnection(), dbType);
        String activeCatalog = getActiveCatalog(metadata.getConnection(), dbType);
        log.debug("Searching for column '{}' in table '{}' under schema '{}' catalog '{}'", columnName, tableName,
                activeSchema, activeCatalog);
        try (ResultSet rs = metadata.getColumns(activeCatalog, activeSchema, tableName, columnName)) {
            boolean found = rs.next();
            log.debug("column '{}' in table '{}' was found: {}", columnName, tableName, found);
            return found;
        }
    }

    public static int getColumnSize(JdbcHelper jdbcHelper, DbType dbType, String tableName, String columnName)
            throws SQLException {
        return withMetadata(jdbcHelper, metadata -> getColumnSize(metadata, dbType, tableName, columnName));
    }

    public static int getColumnSize(DatabaseMetaData metadata, DbType dbType, String tableName, String columnName)
            throws SQLException {
        String normalizedColumnName = normalizedName(columnName, metadata);
        String normalizedTableName = normalizedName(tableName, metadata);
        String activeSchema = getActiveSchema(metadata.getConnection(), dbType);
        String activeCatalog = getActiveCatalog(metadata.getConnection(), dbType);
        log.debug("Searching for column '{}' in table '{}' under schema '{}' catalog '{}'", normalizedColumnName,
                normalizedTableName, activeSchema, activeCatalog);
        try (ResultSet rs = metadata
                .getColumns(activeCatalog, activeSchema, normalizedTableName, normalizedColumnName)) {
            if (rs.next()) {
                log.debug("column '{}' in table '{}' was found", normalizedColumnName, normalizedTableName);
                return rs.getInt(COLUMN_SIZE);
            }
        }
        throw new RuntimeException(
                "Column: " + normalizedColumnName + " was not found on table: " + normalizedTableName);
    }

    /**
     * Tests {@param tableName} for the existence of {@param indexName} and if it is set on {@param columnName} if the
     * latter was passed. For complex indices you may pass null in {@param columnName} to have the method skip this
     * validation.
     */
    public static boolean indexExists(JdbcHelper jdbcHelper, String tableName, @Nullable String columnName,
            String indexName, DbType dbType) throws SQLException {
        return withConnection(jdbcHelper,
                conn -> indexExists(jdbcHelper, conn, dbType, tableName, columnName, indexName, false));
    }

    public static boolean indexExists(JdbcHelper jdbcHelper, Connection conn, DbType dbType, String tableName,
            @Nullable String columnName, String indexName, boolean strictDerby) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        String normalizedTableName = normalizedName(tableName, metadata);
        String normalizedColName = (isBlank(columnName) ? columnName : normalizedName(columnName, metadata));
        String normalizedIndexName = normalizedName(indexName, metadata);
        if (!tableExists(metadata, dbType, tableName) || !columnExists(metadata, dbType, tableName, columnName)) {
            return false;
        }
        String activeCatalog = getActiveCatalog(conn, dbType);
        String activeSchema = getActiveSchema(conn, dbType);
        if (DbType.DERBY.equals(dbType)) {
            return indexExistsDerby(metadata, activeCatalog, activeSchema, normalizedTableName, normalizedColName,
                    normalizedIndexName, strictDerby);
        } else if (DbType.ORACLE.equals(dbType)) {
            return indexExistsOracle(jdbcHelper, activeSchema, normalizedTableName, normalizedColName,
                    normalizedIndexName);
        } else {
            return indexExists(metadata, activeCatalog, activeSchema, normalizedTableName, normalizedColName,
                    normalizedIndexName);
        }
    }

    private static boolean indexExistsOracle(JdbcHelper jdbcHelper,
            String activeSchema,
            String normalizedTableName, String normalizedColName, String normalizedIndexName) throws SQLException {
        int i = jdbcHelper.executeSelectCount("SELECT COUNT(*) FROM ALL_IND_COLUMNS WHERE " +
                "INDEX_NAME = ? AND " +
                "COLUMN_NAME = ? AND " +
                "TABLE_NAME = ? AND " +
                "INDEX_OWNER = ? ", normalizedIndexName, normalizedColName, normalizedTableName, activeSchema);
        return i > 0;
    }

    private static boolean indexExists(DatabaseMetaData metadata, String activeCatalog, String activeSchema,
            String tableName, @Nullable String columnName, String indexName) throws SQLException {
        String colNameColumn = normalizedName("COLUMN_NAME", metadata);
        String idxNameColumn = normalizedName("INDEX_NAME", metadata);
        String tblNameColumn = normalizedName("TABLE_NAME", metadata);
        try (ResultSet rs = metadata.getIndexInfo(activeCatalog, activeSchema, tableName, false, false)) {
            return indexExists(tableName, columnName, indexName, colNameColumn, idxNameColumn, tblNameColumn, rs);
        }
    }

    private static boolean indexExists(String tableName, @Nullable String columnName, String indexName,
            String colNameColumn, String idxNameColumn, String tblNameColumn, ResultSet rs) throws SQLException {
        while (rs.next()) {
            if (indexName.equals(rs.getString(idxNameColumn)) && tableName.equals(rs.getString(tblNameColumn))) {
                if (isNotBlank(columnName)) {
                    // This validation depends on whether column name was passed or not, if not then the above is enough
                    // to determine the index existence.
                    if (columnName.equals(rs.getString(colNameColumn))) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Specifically in derby we're trying the 'regular' way first, but since it sucks (see comment in #indexExistsDerby)
     * There's also a fallback to derby-specific semi-correct logic (for primary key indices)
     *
     * @param strict - Strictness means we only search indices by name or use the partial-logic that just matches the
     *               column
     */
    private static boolean indexExistsDerby(DatabaseMetaData metadata, String activeCatalog, String activeSchema,
            String tableName, String columnName, String indexName, boolean strict) throws SQLException {
        if (indexExists(metadata, activeCatalog, activeSchema, tableName, columnName, indexName)) {
            return true;
        } else if (!strict) {
            if (isBlank(columnName)) {
                log.warn("Can't search for an index on Derby without column name");
                return false;
            }
            return indexExistsDerby(metadata, activeCatalog, activeSchema, tableName, columnName);
        }
        return false;
    }

    /**
     * Scumbag Derby has to do everything differently, when you put an index on a primary key it drops the name you
     * gave it and gives it its own name based on its uniqueness scheme (i.e. SQL181215003159240)
     * {@see https://stackoverflow.com/questions/53908174/apache-derby-gives-strange-names-to-indices-i-created-with-meaningful-names}
     *
     * This is the old (and very lacking) implementation that simply assumes that if the column we're looking for has
     * *any* index, then that's good enough.
     * In reality of course This is a very problematic assumption since a column can be a member in several indices.
     */
    private static boolean indexExistsDerby(DatabaseMetaData metadata, String activeCatalog, String activeSchema,
            String tableName, String columnName) throws SQLException {
        String colNameColumn = normalizedName("COLUMN_NAME", metadata);
        try (ResultSet rs = metadata.getIndexInfo(activeCatalog, activeSchema, tableName, false, false)) {
            while (rs.next()) {
                if (rs.getString(colNameColumn).equals(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests for the existence of {@param foreignKeyName} in {@param tableName}. Also verifies the table exists.
     *
     * @return true if foreign key exists.
     */
    public static boolean foreignKeyExists(JdbcHelper jdbcHelper, DbType dbType, String tableName,
            String foreignKeyName) throws SQLException {
        return withConnection(jdbcHelper, conn -> foreignKeyExists(conn, dbType, tableName, foreignKeyName));
    }

    private static boolean foreignKeyExists(Connection conn, DbType dbType, String tableName, String foreignKeyName)
            throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        tableName = normalizedName(tableName, metadata);
        foreignKeyName = normalizedName(foreignKeyName, metadata);
        if (isBlank(tableName) || isBlank(foreignKeyName)) {
            throw new IllegalStateException("Could not resolve db-specific identifier names");
        }
        if (!tableExists(metadata, dbType, tableName)) {
            return false;
        }
        return foreignKeyExists(metadata, getActiveCatalog(conn, dbType), getActiveSchema(conn, dbType), tableName,
                foreignKeyName);
    }

    /**
     * Send me normalized names!
     */
    private static boolean foreignKeyExists(DatabaseMetaData metadata, String activeCatalog, String activeSchema,
            String tableName, String keyName) throws SQLException {
        String fkNameColumn = normalizedName("FK_NAME", metadata);
        String fkTableColumn = normalizedName("FKTABLE_NAME", metadata);
        try (ResultSet rs = metadata.getImportedKeys(activeCatalog, activeSchema, tableName)) {
            while (rs.next()) {
                if (rs.getString(fkNameColumn).equals(keyName) && rs.getString(fkTableColumn).equals(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getDbTypeNameForSqlResources(DbType dbType) {
        String dbTypeName = dbType.name();
        if (dbTypeName.equals(DbType.MARIADB.name())) {
            log.debug("Identified MariaDB as DB type, using MySql conversion files");
            return DbType.MYSQL.name().toLowerCase();
        } else {
            return dbTypeName.toLowerCase();
        }
    }

    public static void doWithConnection(JdbcHelper jdbcHelper, SQLThrowingConsumer<Connection, SQLException> whatToDo)
            throws SQLException {
        DataSource ds = null;
        Connection conn = null;
        try {
            ds = jdbcHelper.getDataSource();
            conn = ds.getConnection();
            whatToDo.accept(conn);
        } finally {
            DbUtils.close(conn, ds);
        }
    }

    public static void doStreamWithConnection(JdbcHelper jdbcHelper,
            IOSQLThrowingConsumer<Connection, SQLException, IOException> whatToDo) throws IOException, SQLException {
        DataSource ds = null;
        Connection conn = null;
        try {
            ds = jdbcHelper.getDataSource();
            conn = ds.getConnection();
            whatToDo.accept(conn);
        } finally {
            DbUtils.close(conn, ds);
        }
    }

    public static <T> T withConnection(JdbcHelper jdbcHelper, SQLThrowingFunction<Connection, T, SQLException> whatToDo)
            throws SQLException {
        DataSource ds = null;
        Connection conn = null;
        try {
            ds = jdbcHelper.getDataSource();
            conn = ds.getConnection();
            return whatToDo.apply(conn);
        } finally {
            DbUtils.close(conn, ds);
        }
    }

    public static <T> T withMetadata(JdbcHelper jdbcHelper,
            SQLThrowingFunction<DatabaseMetaData, T, SQLException> whatToDo) throws SQLException {
        DataSource ds = null;
        Connection conn = null;
        try {
            ds = jdbcHelper.getDataSource();
            conn = ds.getConnection();
            return whatToDo.apply(conn.getMetaData());
        } finally {
            DbUtils.close(conn, ds);
        }
    }

    /**
     * I'm a utility for debug time, don't remove me :(
     * This guy prints everything csv-style so you can paste it in any text editor that supports viewing csv files
     */
    public static String printResultSet(ResultSet resultSet) {
        StringBuilder resultBuilder = new StringBuilder();
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            for (int i = 1; i <= columnsNumber; i++) {
                resultBuilder.append(rsmd.getColumnName(i)).append(",");
            }
            resultBuilder.append("\n");
            while (resultSet.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    resultBuilder.append(resultSet.getString(i)).append(",");
                }
                resultBuilder.append("\n");
            }
            resultBuilder.append("\n");
        } catch (Exception e) {
            log.error("", e);
        }
        return resultBuilder.toString();
    }
}
