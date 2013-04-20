package com.ryanjustus.sshdatasource;

import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created with IntelliJ IDEA.
 * User: ryan
 * Time: 4/20/13 3:02 PM
 */
public class SshDataSourceTest {
	@org.junit.Test
	public void testSetRemoteUrl() throws Exception {

	}

	@Test
	public void testConnect() throws SQLException {
		SshDataSource ds = new SshDataSource("scraper@ls.ekiwi.net", "sshpass");
		// SshDataSource
		ds.setDriverClassName( "com.mysql.jdbc.Driver" );
		ds.setUsername( "root" );
		ds.setPassword( "dbpass" );
		ds.setUrl( SshDataSource.Type.MYSQL, 3306, "test" );

		Connection con=null;

		try{
			con = ds.getConnection();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT pk FROM test");
			if(rs.next()){
				System.out.println(rs.getInt("pk"));
			}
		}finally{
			con.close();
		}
		ds.close();
	}
}
