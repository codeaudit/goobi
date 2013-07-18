package de.sub.goobi.persistence.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.Template;
import org.goobi.managedbeans.LoginBean;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;

class ProcessMysqlHelper {
    private static final Logger logger = Logger.getLogger(ProcessMysqlHelper.class);

    public static Process getProcessById(int id) throws SQLException {
        Connection connection = null;
        String sql = "SELECT * FROM prozesse WHERE ProzesseID = ?";
        Object[] param = { id };
        try {
            connection = MySQLHelper.getInstance().getConnection();
            Process p = new QueryRunner().query(connection, sql, resultSetToProcessHandler, param);
            return p;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void saveProcess(Process o, boolean processOnly) throws DAOException {
        try {
            o.setSortHelperStatus(o.getFortschritt());

            if (o.getId() == null) {
                // new process
                insertProcess(o);

            } else {
                // process exists already in database
                updateProcess(o);
            }

            if (!processOnly) {
                List<Step> stepList = o.getSchritte();
                for (Step s : stepList) {
                    StepMysqlHelper.saveStep(s);
                }
            }
            List<Processproperty> properties = o.getEigenschaften();
            for (Processproperty pe : properties) {
                PropertyManager.saveProcessProperty(pe);
            }

            for (Masterpiece object : o.getWerkstuecke()) {
                MasterpieceManager.saveMasterpiece(object);
            }

            List<Template> templates = o.getVorlagen();
            for (Template template : templates) {
                TemplateManager.saveTemplate(template);
            }

        } catch (SQLException e) {
            //            logger.error("Error while saving process " + o.getTitel(), e);
            throw new DAOException(e);
        }
    }

    public static void deleteProcess(Process o) throws SQLException {
        if (o.getId() != null) {

            // delete properties

            for (Processproperty object : o.getEigenschaften()) {
                PropertyManager.deleteProcessProperty(object);
            }
            // delete templates

            for (Template object : o.getVorlagen()) {
                TemplateManager.deleteTemplate(object);
            }

            // delete masterpieces
            for (Masterpiece object : o.getWerkstuecke()) {
                MasterpieceManager.deleteMasterpiece(object);
            }

            // delete process
            String sql = "DELETE FROM prozesse WHERE ProzesseID = ?";
            Object[] param = { o.getId() };
            Connection connection = null;
            try {
                connection = MySQLHelper.getInstance().getConnection();
                QueryRunner run = new QueryRunner();
                run.update(connection, sql, param);
            } finally {
                if (connection != null) {
                    MySQLHelper.closeConnection(connection);
                }
            }
        }
    }

    public static int getProcessCount(String order, String filter) throws SQLException {

        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(ProzesseID) FROM prozesse, projekte WHERE prozesse.ProjekteID = projekte.ProjekteID ");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" AND " + filter);
        }
        try {
            connection = MySQLHelper.getInstance().getConnection();
            logger.debug(sql.toString());
            if (filter != null && !filter.isEmpty()) {
                return new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerHandler);
            } else {
                return new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerHandler);
            }
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<Process> getProcesses(String order, String filter, Integer start, Integer count) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM prozesse, projekte WHERE prozesse.ProjekteID = projekte.ProjekteID ");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" AND " + filter);
        }
        if (order != null && !order.isEmpty()) {
            sql.append(" ORDER BY " + order);
        }
        if (start != null && count != null) {
            sql.append(" LIMIT " + start + ", " + count);
        }
        try {
            connection = MySQLHelper.getInstance().getConnection();
            logger.debug(sql.toString());
            List<Process> ret = null;
                ret = new QueryRunner().query(connection, sql.toString(), resultSetToProcessListHandler);
            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }
    
    
    public static List<Integer> getProcessIdList(String order, String filter, Integer start, Integer count) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT prozesse.ProzesseID FROM prozesse, projekte WHERE prozesse.ProjekteID = projekte.ProjekteID ");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" AND " + filter);
        }
        if (order != null && !order.isEmpty()) {
            sql.append(" ORDER BY " + order);
        }
        if (start != null && count != null) {
            sql.append(" LIMIT " + start + ", " + count);
        }
        try {
            connection = MySQLHelper.getInstance().getConnection();
            logger.debug(sql.toString());
            List<Integer> ret = null;
                ret = new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerListHandler);
            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<Process> getAllProcesses() throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM prozesse");
        try {
            connection = MySQLHelper.getInstance().getConnection();
            logger.debug(sql.toString());
            List<Process> ret = new QueryRunner().query(connection, sql.toString(), resultSetToProcessListHandler);
            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static ResultSetHandler<List<Process>> resultSetToProcessListHandler = new ResultSetHandler<List<Process>>() {
        public List<Process> handle(ResultSet rs) throws SQLException {
            List<Process> answer = new ArrayList<Process>();
            try {
                while (rs.next()) {
                    try {
                        Process o = convert(rs);
                        if (o != null) {
                            answer.add(o);
                        }
                    } catch (DAOException e) {
                        logger.error(e);
                    }
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
            return answer;
        }
    };

    public static ResultSetHandler<Process> resultSetToProcessHandler = new ResultSetHandler<Process>() {
        public Process handle(ResultSet rs) throws SQLException {
            try {
                if (rs.next()) {
                    try {
                        Process o = convert(rs);
                        return o;
                    } catch (DAOException e) {
                        logger.error(e);
                    }
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
            return null;
        }
    };

    private static void insertProcess(Process o) throws SQLException {
        String sql = "INSERT INTO prozesse " + generateInsertQuery(false) + generateValueQuery(false);
        Object[] param = generateParameter(o, false, false);
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            logger.debug(sql.toString());
            Integer id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, param);
            if (id != null) {
                o.setId(id);
            }
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static String generateInsertQuery(boolean includeProcessId) {
        if (!includeProcessId) {
            return "(Titel, ausgabename, IstTemplate, swappedOut, inAuswahllisteAnzeigen, sortHelperStatus,"
                    + "sortHelperImages, sortHelperArticles, erstellungsdatum, ProjekteID, MetadatenKonfigurationID, sortHelperDocstructs,"
                    + "sortHelperMetadata, wikifield, batchID, docketID)" + " VALUES ";
        } else {
            return "(ProzesseID, Titel, ausgabename, IstTemplate, swappedOut, inAuswahllisteAnzeigen, sortHelperStatus,"
                    + "sortHelperImages, sortHelperArticles, erstellungsdatum, ProjekteID, MetadatenKonfigurationID, sortHelperDocstructs,"
                    + "sortHelperMetadata, wikifield, batchID, docketID)" + " VALUES ";
        }
    }

    private static String generateValueQuery(boolean includeProcessId) {
        if (!includeProcessId) {
            return "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            return "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
    }

    private static Object[] generateParameter(Process o, boolean createNewTimestamp, boolean includeProcessID) {
        Date d = null;
        if (createNewTimestamp) {
            d = new Date();
        } else {
            d = o.getErstellungsdatum();
        }
        if (o.getProjectId() == null && o.getProjekt() != null) {
            o.setProjectId(o.getProjekt().getId());
        }

        Timestamp datetime = new Timestamp(d.getTime());
        if (!includeProcessID) {
            Object[] param =
                    { o.getTitel(), o.getAusgabename(), o.isIstTemplate(), o.isSwappedOutHibernate(), o.isInAuswahllisteAnzeigen(),
                            o.getSortHelperStatus(), o.getSortHelperImages(), o.getSortHelperArticles(), datetime, o.getProjectId(),
                            o.getRegelsatz().getId(), o.getSortHelperDocstructs(), o.getSortHelperMetadata(),
                            o.getWikifield().equals("") ? " " : o.getWikifield(), o.getBatchID(),
                            o.getDocket() == null ? null : o.getDocket().getId() };

            return param;
        } else {
            Object[] param =
                    { o.getId(), o.getTitel(), o.getAusgabename(), o.isIstTemplate(), o.isSwappedOutHibernate(), o.isInAuswahllisteAnzeigen(),
                            o.getSortHelperStatus(), o.getSortHelperImages(), o.getSortHelperArticles(), datetime,  o.getProjectId(),
                            o.getRegelsatz().getId(), o.getSortHelperDocstructs(), o.getSortHelperMetadata(),
                            o.getWikifield().equals("") ? " " : o.getWikifield(), o.getBatchID(),
                            o.getDocket() == null ? null : o.getDocket().getId() };

            return param;
        }
    }

    private static void updateProcess(Process o) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE prozesse SET ");
        sql.append(" Titel = ?,");
        sql.append(" ausgabename = ?,");
        sql.append(" IstTemplate = ?,");
        sql.append(" swappedOut = ?,");
        sql.append(" inAuswahllisteAnzeigen = ?,");
        sql.append(" sortHelperStatus = ?,");
        sql.append(" sortHelperImages = ?,");
        sql.append(" sortHelperArticles = ?,");
        sql.append(" erstellungsdatum = ?,");
        sql.append(" ProjekteID = ?,");
        sql.append(" MetadatenKonfigurationID = ?,");
        sql.append(" sortHelperDocstructs = ?,");
        sql.append(" sortHelperMetadata = ?,");
        sql.append(" wikifield = ?,");
        sql.append(" batchID = ?,");
        sql.append(" docketID = ?");
        sql.append(" WHERE ProzesseID = " + o.getId());

        Object[] param = generateParameter(o, false, false);

        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            run.update(connection, sql.toString(), param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static Process convert(ResultSet rs) throws DAOException, SQLException {
        Process p = new Process();
        p.setId(rs.getInt("ProzesseID"));
        p.setTitel(rs.getString("Titel"));
        p.setAusgabename(rs.getString("ausgabename"));
        p.setIstTemplate(rs.getBoolean("IstTemplate"));
        p.setSwappedOutHibernate(rs.getBoolean("swappedOut"));
        p.setInAuswahllisteAnzeigen(rs.getBoolean("inAuswahllisteAnzeigen"));
        p.setSortHelperStatus(rs.getString("sortHelperStatus"));
        p.setSortHelperImages(rs.getInt("sortHelperImages"));
        p.setSortHelperArticles(rs.getInt("sortHelperArticles"));
        Timestamp time = rs.getTimestamp("erstellungsdatum");
        if (time != null) {
            p.setErstellungsdatum(new Date(time.getTime()));
        }
        p.setProjectId(rs.getInt("ProjekteID"));
        p.setRegelsatz(RulesetManager.getRulesetById(rs.getInt("MetadatenKonfigurationID")));
        p.setSortHelperDocstructs(rs.getInt("sortHelperDocstructs"));
        p.setSortHelperMetadata(rs.getInt("sortHelperMetadata"));
        p.setWikifield(rs.getString("wikifield"));
        Integer batchID = rs.getInt("batchID");
        if (rs.wasNull()) {
            batchID = null;
        }
        p.setBatchID(batchID);

        p.setDocket(DocketManager.getDocketById(rs.getInt("docketID")));

        return p;
    }

    public static void insertBatchProcessList(List<Process> processList) throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO prozesse " + generateInsertQuery(false));
        List<Object[]> paramArray = new ArrayList<Object[]>();
        for (Process o : processList) {
            sql.append(" " + generateValueQuery(false) + ",");
            Object[] param = generateParameter(o, false, false);
            paramArray.add(param);
        }
        String values = sql.toString();

        values = values.substring(0, values.length() - 1);

        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            run.update(connection, values, paramArray);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void updateBatchList(List<Process> processList) throws SQLException {

        String tablename = "a" + String.valueOf(new Date().getTime());
        String tempTable = "CREATE TEMPORARY TABLE IF NOT EXISTS " + tablename + " LIKE prozesse;";

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO " + tablename + " " + generateInsertQuery(true));
        List<Object[]> paramList = new ArrayList<Object[]>();
        for (Process o : processList) {
            sql.append(" " + generateValueQuery(true) + ",");
            Object[] param = generateParameter(o, true, true);
            paramList.add(param);
        }
        Object[][] paramArray = new Object[paramList.size()][];
        paramList.toArray(paramArray);

        String insertQuery = sql.toString();
        insertQuery = insertQuery.substring(0, insertQuery.length() - 1);

        StringBuilder joinQuery = new StringBuilder();
        joinQuery.append("UPDATE prozesse SET prozesse.Titel = " + tablename + ".Titel, prozesse.ausgabename = " + tablename + ".ausgabename,"
                + "prozesse.IstTemplate = " + tablename + ".IstTemplate, " + "prozesse.swappedOut = " + tablename + ".swappedOut, "
                + "prozesse.inAuswahllisteAnzeigen = " + tablename + ".inAuswahllisteAnzeigen, " + "prozesse.sortHelperStatus = " + tablename
                + ".sortHelperStatus, " + "prozesse.sortHelperImages = " + tablename + ".sortHelperImages, " + "prozesse.sortHelperArticles = "
                + tablename + ".sortHelperArticles, " + "prozesse.erstellungsdatum = " + tablename + ".erstellungsdatum, " + "prozesse.ProjekteID = "
                + tablename + ".ProjekteID, " + "prozesse.MetadatenKonfigurationID = " + tablename + ".MetadatenKonfigurationID, "
                + "prozesse.sortHelperDocstructs = " + tablename + ".sortHelperDocstructs, " + "prozesse.sortHelperMetadata = " + tablename
                + ".sortHelperMetadata, " + "prozesse.wikifield = " + tablename + ".wikifield, " + "prozesse.batchID = " + tablename + ".batchID, "
                + "prozesse.docketID = " + tablename + ".docketID " + " WHERE prozesse.ProzesseID = " + tablename + ".ProzesseID;");

        String deleteTempTable = "DROP TEMPORARY TABLE " + tablename + ";";

        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            // create temporary table
            run.update(connection, tempTable);
            // insert bulk into a temp table
            run.batch(connection, insertQuery, paramArray);
            // update process table using join
            run.update(connection, joinQuery.toString());
            // delete temporary table
            run.update(connection, deleteTempTable);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        // born digital
        Process p1 = ProcessMysqlHelper.getProcessById(28);
        // MOH
        Process p2 = ProcessMysqlHelper.getProcessById(31);

        List<Process> pl = new ArrayList<Process>();
        pl.add(p1);
        pl.add(p2);
        ProcessMysqlHelper.updateBatchList(pl);
        //        updateBatchList

    }

    public static int getMaxBatchNumber() throws SQLException {
        Connection connection = null;
        String sql = "SELECT max(batchId) FROM prozesse";
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToIntegerHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<Integer> getIDList(String filter) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT prozesseID FROM prozesse");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        }

        try {
            connection = MySQLHelper.getInstance().getConnection();
            logger.debug(sql.toString());
            List<Integer> ret = null;
            ret = new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerListHandler);

            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }

    }

    public static int countProcesses(String filter) throws SQLException {
        String sql = "select count(prozesseID) from prozesse ";
        if (filter != null && filter.length() > 0) {
            sql += " WHERE " + filter;
        }
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToIntegerHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<Integer> getBatchIds(int limit) throws SQLException {
        String sql = "SELECT distinct batchID FROM prozesse";

        LoginBean login = (LoginBean) Helper.getManagedBeanValue("#{LoginForm}");
        if (login != null && login.getMaximaleBerechtigung() > 1) {
            sql +=
                    " WHERE prozesse.ProjekteID in (SELECT ProjekteID FROM projektbenutzer WHERE projektbenutzer.BenutzerID = "
                            + login.getMyBenutzer().getId() + ")";
        }

        sql += " ORDER BY batchID desc ";
        if (limit > 0) {
            sql += " limit " + limit;
        }
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToIntegerListHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List runSQL(String sql) throws SQLException {
        Connection connection = null;
        List answer = new ArrayList();
        try {
            connection = MySQLHelper.getInstance().getConnection();
            Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = stmt.executeQuery(sql);
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getString(i);
                }
                answer.add(row);
            }
            return answer;

        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void updateImages(Integer numberOfFiles, int processId) throws SQLException {
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            StringBuilder sql = new StringBuilder();
            Object[] param = { numberOfFiles, processId };
            sql.append("UPDATE prozesse SET sortHelperImages = ? WHERE ProzesseID = ?");
            logger.debug(sql.toString() + ", " + param);
            run.update(connection, sql.toString(), param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void updateProcessLog(String logValue, int processId) throws SQLException {
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            StringBuilder sql = new StringBuilder();
            Object[] param = { logValue, processId };
            sql.append("UPDATE prozesse SET wikifield = ? WHERE ProzesseID = ?");
            logger.debug(sql.toString() + ", " + param);
            run.update(connection, sql.toString(), param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void updateProcessStatus(String value, int processId) throws SQLException {
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            StringBuilder sql = new StringBuilder();
            Object[] param = { value, processId };
            sql.append("UPDATE prozesse SET sortHelperStatus = ? WHERE ProzesseID = ?");
            logger.debug(sql.toString() + ", " + param);
            run.update(connection, sql.toString(), param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static int getCountOfProcessesWithRuleset(int rulesetId) throws SQLException {
        Connection connection = null;

        String query = "select count(ProzesseID) from prozesse where MetadatenKonfigurationID = ?";
        try {
            connection = MySQLHelper.getInstance().getConnection();
            Object[] param = { rulesetId };
            return new QueryRunner().query(connection, query, MySQLHelper.resultSetToIntegerHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static int getCountOfProcessesWithDocket(int docketId) throws SQLException {
        Connection connection = null;
        String query = "select count(ProzesseID) from prozesse where  docketID= ?";
        try {
            connection = MySQLHelper.getInstance().getConnection();
            Object[] param = { docketId };
            return new QueryRunner().query(connection, query, MySQLHelper.resultSetToIntegerHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static int getCountOfProcessesWithTitle(String title) throws SQLException {
        Connection connection = null;
        String query = "select count(prozesse.ProzesseID) from prozesse where  titel = ?";
        try {
            connection = MySQLHelper.getInstance().getConnection();
            Object[] param = { title };
            return new QueryRunner().query(connection, query, MySQLHelper.resultSetToIntegerHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static long getSumOfFieldValue(String columnname, String filter) throws SQLException {
        String sql = "select sum(prozesse." + columnname + ") from prozesse ";
        if (filter != null && filter.length() > 0) {
            sql += " WHERE " + filter;
        }
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToLongHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static long getCountOfFieldValue(String columnname, String filter) throws SQLException {
        String sql = "select count(prozesse." + columnname + ") from prozesse ";
        if (filter != null && filter.length() > 0) {
            sql += " WHERE " + filter;
        }
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToLongHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static String getProcessTitle(int processId) throws SQLException {
        String sql = "SELECT titel from prozesse WHERE ProzesseID = " + processId;
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToStringHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }
}