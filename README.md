SshDataSource
=============

extends org.apache.commons.dbcp.BasicDataSource to tunnel it through an ssh connection using the ganymed-ssh-2 library

Example usage:
```java
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
```
