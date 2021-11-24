package bangla.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;

import dbm.*;

public class InputOutputLogDetailDAO  
{
	
	Logger logger = Logger.getLogger(InputOutputLogDetailDAO.class);
	String tableName = "spell_checker_log_detail";
	
	public InputOutputLogDetailDAO()
	{	
	}
	
	public void add(long logid, String action, String content, String newValue) throws Exception
	{
		Connection connection = null;
		PreparedStatement ps = null;

		try{
	
			String sql = "INSERT INTO " + tableName;
			
			sql += " (";
			sql += "logid";
			sql += ", ";
			sql += "action";
			sql += ", ";
			sql += "content";
			sql += ", ";
			sql += "new_value";
			sql += ")";
			
			
            sql += " VALUES(";
			sql += "?";
			sql += ", ";
			sql += "?";
			sql += ", ";
			sql += "?";
			sql += ", ";
			sql += "?";
			sql += ")";

			connection = DBMW.getInstance().getConnection();
			ps = connection.prepareStatement(sql);

			int index = 1;

			ps.setObject(index++,logid);
			ps.setObject(index++,action);
			ps.setObject(index++,content);
			ps.setObject(index++,newValue);
			ps.executeUpdate();

		}finally{
			try{if (ps != null) {ps.close();ps=null;}} catch(Exception e){}
			try{if(connection != null){DBMW.getInstance().freeConnection(connection);connection=null;}}catch(Exception ex2){}
		}
	}
							
}
	