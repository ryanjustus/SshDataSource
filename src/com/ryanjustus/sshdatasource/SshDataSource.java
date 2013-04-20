package com.ryanjustus.sshdatasource;

import ch.ethz.ssh2.Connection;
import java.io.*;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SshDataSource extends BasicDataSource {

	public enum Type{
		MYSQL("MySql"),
		MSSQL("MsSql"),
		ORACLE("Oracle"),
		POSTGRESQL("PostgreSql");

		final String name;
		Type(String name){
			this.name=name;
		}

		public String toString(){
			return name();
		}
	}

	public String remoteUrl = "localhost";
	private Connection sshCon;
	private static Log log = LogFactory.getLog(SshDataSource.class);
	/**
	 * Default using ssh port 22
	 * @param sshUser Ex: user@remotehost.com
	 * @param password
	 */
	public SshDataSource(String sshUser, String password)
	{
		super();
		initWithPass(sshUser, 22, password);
	}
	/**
	 * Creates a BasicDataSource that uses an ssh tunnel to connect to the database
	 * @param sshUser  Ex: user@remotehost.com
	 * @param sshPort  remote ssh server port
	 * @param password ssh connection password
	 */
	public SshDataSource(String sshUser, int sshPort, String password)
	{
		super();
		initWithPass(sshUser, sshPort, password);
	}

	/**
	 * Default using ssh port 22
	 * @param sshUser Ex: user@remotehost.com
	 * @param key ssh keyfile in pem format
	 * @throws FileNotFoundException
	 */
	public SshDataSource(String sshUser, File key) throws FileNotFoundException
	{
		super();
		initWithKey(sshUser, 22, key);
	}

	/**
	 * Creates a BasicDataSource that uses an ssh tunnel to connect to the database
	 * @param sshUser, formatted like 'user@remotehost'
	 * @param sshPort, remote ssh server port
	 * @param key, ssh keyfile in pem format
	 * @throws FileNotFoundException
	 */

	public SshDataSource(String sshUser, int sshPort,File key) throws FileNotFoundException
	{
		super();
		initWithKey(sshUser, sshPort, key);
	}

	/**
	 * Initializes connection based on a username, password
	 * @param sshUser formatted as 'user@remotehost'
	 * @param sshPort
	 * @param password
	 */
	private void initWithPass(String sshUser, int sshPort, String password)
	{
		String[] user = sshUser.split("@");
		String uname = "";
		String host = "";
		if(user.length==2)
		{
			uname = user[0];
			host = user[1];
		}else
			throw new IllegalArgumentException("sshUser must be of the form user@host");
		if(sshPort<1 || sshPort>65535)
			throw new IllegalArgumentException("invalid ssh port");
		log.info("connecting to "+host+"...");
		sshCon = new Connection(host,sshPort);
		try {
			sshCon.connect();
			sshCon.authenticateWithPassword(uname, password);
			log.info("connected");
		} catch (IOException ex) {

			log.error("Error establishing ssh connection", ex);
		}
	}

	private void initWithKey(String sshUser, int sshPort, File key) throws FileNotFoundException
	{
		String[] user = sshUser.split("@");
		String uname = "";
		String host = "";
		if(user.length==2)
		{
			uname = user[0];
			host = user[1];
		}
		else
			throw new IllegalArgumentException("sshUser must be of the form user@host");
		if(sshPort<1 || sshPort>65535)
			throw new IllegalArgumentException("invalid ssh port");
		try {
			sshCon = new Connection(host,sshPort);
			sshCon.authenticateWithPublicKey(uname, key, password);
		} catch (IOException ex) {
			log.error("Error establishing ssh connection", ex);
		}

	}


	@Override
	public void close() throws SQLException
	{
		super.close();
		sshCon.close();
	}

	/**
	 * Use this instead of setUrl(String url).  Makes an sql connection that is
	 * forwarded through the ssh connection to the remote sql server
	 * @param sqlDatabaseType SshDataSource.MYSQL, SshDataSource.MSSQL, SshDataSource.ORACLE, or SshDataSource.POSTRESQL
	 * @param sqlServerPort listening port of remote sql server
	 * @param databaseName name of the database you want to connect to or null
	 */
	public void setUrl(Type sqlDatabaseType, int sqlServerPort, String databaseName)
	{
		if(databaseName == null)
			databaseName = "";
		try {
			int localPort = getFreePort();

			sshCon.createLocalPortForwarder(localPort, remoteUrl, sqlServerPort);
			String sqlUrl = SshDataSource.getSqlConnectionPath(sqlDatabaseType,localPort, databaseName);
			super.setUrl(sqlUrl);
		} catch (IOException ex) {
			log.error("Error establishing port forwarding", ex);
		}
	}


	/**
	 * Sets the remote url you wish to connect to.  This is for if the remote mysql server is not
	 * located on the same computer as the remote ssh
	 * @param url, default 'localhost'
	 */
	public void setRemoteUrl(String url){
		this.remoteUrl=url;
	}


	@Override
	public void setUrl(String url)
	{
		throw new UnsupportedOperationException("use SshDataSource.setUrl(String sqlDatabaseType, int sqlServerPort, String databaseName)");
	}

	private static String getSqlConnectionPath(Type type, int port, String database)
	{
		final String hostpath = "localhost";
		switch(type){
		   case MYSQL:
			return "jdbc:mysql://" + hostpath +":"+port + "/" + database;
		   case MSSQL:
			return "jdbc:jtds:sqlserver://" + hostpath+":" + "/" + database;
		   case ORACLE:
			return "jdbc:oracle:thin:@"+hostpath+ ":" + port+":"+database;
		   case POSTGRESQL:
			return "jdbc:postgresql://" + hostpath + ":" + port + "/" + database;
		   default:
			throw new IllegalArgumentException("unknown database type " + type);
		}
	}


	public static int getFreePort() {
		boolean free=false;
		int port =0;
		while(!free)
		{
			port = (int)(1024+Math.random()*(65535-1024));
			try
			{
				ServerSocket socket = new ServerSocket(port);
				socket.close();
				free=true;
			}
			catch (IOException ex)
			{
			}
		}
		return port;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}