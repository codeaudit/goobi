package de.sub.goobi.persistence.managers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.log4j.Logger;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;

import de.sub.goobi.persistence.apache.MySQLHelper;
import de.sub.goobi.persistence.apache.MySQLUtils;

public class UserMysqlHelper {
	private static final Logger logger = Logger.getLogger(UserMysqlHelper.class);

	public static List<User> getUsers(String order, String filter, Integer start, Integer count) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM benutzer");
		if (filter!=null && !filter.isEmpty()){
			sql.append(" WHERE " + filter);
		}
		if (order!=null && !order.isEmpty()){
			sql.append(" ORDER BY " + order);
		}
		if (start != null && count != null) {
			sql.append(" LIMIT " + start + ", " + count);
		}
		try {
			logger.debug(sql.toString());
			List<User> ret = new QueryRunner().query(connection, sql.toString(), UserManager.resultSetToUserListHandler);
			return ret;
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}

	public static int getUserCount(String order, String filter) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(BenutzerID) FROM benutzer");
		if (filter!=null && !filter.isEmpty()){
			sql.append(" WHERE " + filter);
		}
		try {
			logger.debug(sql.toString());
			return new QueryRunner().query(connection, sql.toString(), MySQLUtils.resultSetToIntegerHandler);
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}

	public static User getUserById(int id) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM benutzer WHERE BenutzerID = " + id);
		try {
			logger.debug(sql.toString());
			User ret = new QueryRunner().query(connection, sql.toString(), UserManager.resultSetToUserHandler);
			return ret;
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}

	public static void saveUser(User ro) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		try {
			QueryRunner run = new QueryRunner();
			StringBuilder sql = new StringBuilder();

			String visible = null;
			if (ro.getIsVisible()!=null){
				visible = "'" + ro.getIsVisible() + "'";
			}
			
			if (ro.getId() == null) {

				String propNames = "Vorname, Nachname, login, passwort, IstAktiv, Standort, metadatensprache, css, mitMassendownload, confVorgangsdatumAnzeigen, Tabellengroesse, sessiontimeout, ldapgruppenID, isVisible, ldaplogin";
				StringBuilder propValues = new StringBuilder();
				propValues.append("'" + ro.getVorname() + "',");
				propValues.append("'" + ro.getNachname() + "',");
				propValues.append("'" + ro.getLogin() + "',");
				propValues.append("'" + ro.getPasswort() + "',");
				propValues.append(ro.isIstAktiv() + ",");
				propValues.append("'" + ro.getStandort() + "',");
				propValues.append("'" + ro.getMetadatenSprache() + "',");
				propValues.append("'" + ro.getCss() + "',");
				propValues.append(ro.isMitMassendownload() + ",");
				propValues.append(ro.isConfVorgangsdatumAnzeigen() + ",");
				propValues.append(ro.getTabellengroesse() + ",");
				propValues.append(ro.getSessiontimeout() + ",");
				propValues.append(ro.getLdapGruppe().getId() + ",");
				propValues.append(visible + ",");
				propValues.append("'" + ro.getLdaplogin() + "'");

				sql.append("INSERT INTO benutzer (");
				sql.append(propNames.toString());
				sql.append(") VALUES (");
				sql.append(propValues);
				sql.append(")");
			} else {
				sql.append("UPDATE benutzer SET ");
				sql.append("Vorname = '" + ro.getVorname() + "',");
				sql.append("Nachname = '" + ro.getNachname() + "',");
				sql.append("login = '" + ro.getLogin() + "',");
				sql.append("passwort = '" + ro.getPasswort() + "',");
				sql.append("IstAktiv = " + ro.isIstAktiv() + ",");
				sql.append("Standort = '" + ro.getStandort() + "',");
				sql.append("metadatensprache = '" + ro.getMetadatenSprache() + "',");
				sql.append("css = '" + ro.getCss() + "',");
				sql.append("mitMassendownload = " + ro.isMitMassendownload() + ",");
				sql.append("confVorgangsdatumAnzeigen = " + ro.isConfVorgangsdatumAnzeigen() + ",");
				sql.append("Tabellengroesse = " + ro.getTabellengroesse() + ",");
				sql.append("sessiontimeout = " + ro.getSessiontimeout() + ",");
				sql.append("ldapgruppenID = " + ro.getLdapGruppe().getId() + ",");
				sql.append("isVisible = " + visible + ",");
				sql.append("ldaplogin = '" + ro.getLdaplogin() + "'");
				sql.append(" WHERE BenutzerID = " + ro.getId() + ";");
			}
			logger.debug(sql.toString());
			run.update(connection, sql.toString());
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}

	public static void hideUser(User ro) throws SQLException {
		if (ro.getId() != null) {
			Connection connection = MySQLHelper.getInstance().getConnection();
			try {
				QueryRunner run = new QueryRunner();
				String sql = "UPDATE benutzer SET isVisible = 'deleted' WHERE BenutzerID = " + ro.getId() + ";";
				logger.debug(sql);
				run.update(connection, sql);
			} finally {
				MySQLHelper.closeConnection(connection);
			}
		}
	}
	
	public static void deleteUser(User ro) throws SQLException {
		if (ro.getId() != null) {
			Connection connection = MySQLHelper.getInstance().getConnection();
			try {
				QueryRunner run = new QueryRunner();
				String sql = "DELETE FROM benutzer WHERE BenutzerID = " + ro.getId() + ";";
				logger.debug(sql);
				run.update(connection, sql);
			} finally {
				MySQLHelper.closeConnection(connection);
			}
		}
	}
	
	public static List<String> getFilterForUser(int userId) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM benutzereigenschaften WHERE Titel = '_filter' AND BenutzerID = ?");
		try {
			Object[] param = { userId };
			logger.debug(sql.toString() + ", " + param);
			List<String> answer = new QueryRunner().query(connection, sql.toString(), MySQLUtils.resultSetToFilterListtHandler, param);
			return answer;
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}	
	
	public static void addFilterToUser(int userId, String filterstring) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		Timestamp datetime = new Timestamp(new Date().getTime());
		try {
			QueryRunner run = new QueryRunner();
			String propNames = "Titel, Wert, IstObligatorisch, DatentypenID, Auswahl, creationDate, BenutzerID";
			Object[] param = { "_filter", filterstring, false, 5, null, datetime, userId };
			String sql = "INSERT INTO " + "benutzereigenschaften" + " (" + propNames + ") VALUES ( ?, ?,? ,? ,? ,?,? )";
			logger.debug(sql.toString() + ", " + param);
			run.update(connection, sql, param);
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}

	public static void removeFilterFromUser(int userId, String filterstring) throws SQLException {
		Connection connection = MySQLHelper.getInstance().getConnection();
		try {
			QueryRunner run = new QueryRunner();
			Object[] param = { userId, filterstring };
			String sql = "DELETE FROM benutzereigenschaften WHERE Titel = '_filter' AND BenutzerID = ? AND Wert = ?";
			logger.debug(sql.toString() + ", " + param);
			run.update(connection, sql, param);
		} finally {
			MySQLHelper.closeConnection(connection);
		}
	}

	public static List<User> getUsersForUsergroup(Usergroup usergroup) throws SQLException {
		return getUsers("Nachname,Vorname","BenutzerID IN (SELECT BenutzerID FROM benutzergruppenmitgliedschaft WHERE BenutzerGruppenID=" + usergroup.getId() +")",null,null);
	}
}