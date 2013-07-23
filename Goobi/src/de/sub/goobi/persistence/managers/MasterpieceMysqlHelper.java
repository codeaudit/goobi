package de.sub.goobi.persistence.managers;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Masterpieceproperty;

class MasterpieceMysqlHelper implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -4634402187657044322L;

    public static List<Masterpiece> getMasterpiecesForProcess(int processId) throws SQLException {
        Connection connection = null;

        String sql = " SELECT * from werkstuecke WHERE ProzesseID = " + processId;

        try {
            connection = MySQLHelper.getInstance().getConnection();
            List<Masterpiece> ret = new QueryRunner().query(connection, sql.toString(), resultSetToMasterpieceListHandler);
            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }

    }

    public static ResultSetHandler<List<Masterpiece>> resultSetToMasterpieceListHandler = new ResultSetHandler<List<Masterpiece>>() {
        @Override
        public List<Masterpiece> handle(ResultSet rs) throws SQLException {
            List<Masterpiece> answer = new ArrayList<Masterpiece>();
            try {
                while (rs.next()) {
                    int id = rs.getInt("WerkstueckeID");
                    int processId = rs.getInt("ProzesseID");
                    Masterpiece object = new Masterpiece();
                    object.setId(id);
                    object.setProcessId(processId);
                    List<Masterpieceproperty> properties = PropertyManager.getMasterpieceProperties(id);
                    object.setEigenschaften(properties);
                    answer.add(object);
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
            return answer;
        }
    };

    public static ResultSetHandler<Masterpiece> resultSetToMasterpieceHandler = new ResultSetHandler<Masterpiece>() {
        @Override
        public Masterpiece handle(ResultSet rs) throws SQLException {
            try {
                if (rs.next()) {
                    int id = rs.getInt("WerkstueckeID");
                    int processId = rs.getInt("ProzesseID");
                    Masterpiece object = new Masterpiece();
                    object.setId(id);
                    object.setProcessId(processId);
                    List<Masterpieceproperty> properties = PropertyManager.getMasterpieceProperties(id);
                    object.setEigenschaften(properties);
                    return object;
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
            return null;
        }
    };

    public static Masterpiece getMasterpieceForTemplateID(int templateId) throws SQLException {
        Connection connection = null;
        String sql = " SELECT * from werkstuecke WHERE WerkstueckeID = " + templateId;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            Masterpiece ret = new QueryRunner().query(connection, sql.toString(), resultSetToMasterpieceHandler);
            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static int countMasterpieces() throws SQLException {
        Connection connection = null;
        String sql = " SELECT count(WerkstueckeID) from werkstuecke";
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void saveMasterpiece(Masterpiece object) throws SQLException {
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            String sql = "";

            if (object.getId() == null) {
                sql = "INSERT INTO werkstuecke (  ProzesseID ) VALUES ( ?)";
                Object[] param = { object.getProzess().getId() };

                int id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, param);
                object.setId(id);
            } else {
                sql = "UPDATE werkstuecke set  ProzesseID = ? WHERE WerkstueckeID =" + object.getId();
                Object[] param = { object.getProzess().getId() };
                run.update(connection, sql, param);
            }

        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }

        List<Masterpieceproperty> templateProperties = object.getEigenschaften();
        for (Masterpieceproperty property : templateProperties) {
            property.setMasterpieceId(object.getId());
            property = PropertyManager.saveMasterpieceProperty(property);
        }
    }

    public static void deleteMasterpiece(Masterpiece object) throws SQLException {
        if (object.getId() != null) {
            for (Masterpieceproperty property : object.getEigenschaften()) {
                PropertyManager.deleteMasterpieceProperty(property);
            }
            Connection connection = null;
            try {
                connection = MySQLHelper.getInstance().getConnection();
                QueryRunner run = new QueryRunner();
                String sql = "DELETE FROM werkstuecke WHERE WerkstueckeID = " + object.getId();
                run.update(connection, sql);
            } finally {
                if (connection != null) {
                    MySQLHelper.closeConnection(connection);
                }
            }
        }
    }
}