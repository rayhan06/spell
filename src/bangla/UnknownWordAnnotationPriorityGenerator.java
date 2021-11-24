package bangla;


/*
  
 /usr/jdk1.8.0_161/bin/java -Xmx10240m -classpath /usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/DBManager.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/log4j-1.2.17.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/mysql-connector-java-8.0.17.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/classes   bangla.UnknownWordAnnotationPriorityGenerator > UnknownWordAnnotationPriorityGeneration.txt &

 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;
import bangla.dao.trie.TrieRepository;
import config.GlobalConfigurationRepository;
import dbm.DBMR;
import dbm.DBMW;
import repository.RepositoryManager;


public class UnknownWordAnnotationPriorityGenerator 
{
	static Logger logger = Logger.getLogger(UnknownWordAnnotationPriorityGenerator.class);

	public static void main(String args[]) throws Exception
	{
		System.out.println("Starting UnknownWordAnnotationPriorityGenerator ");
		Connection connection = null, connectionW=null;
		Statement stmt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		long startTime = System.currentTimeMillis();
		
		logger.debug("Loading Startup Repository");
		RepositoryManager.getInstance();
		GlobalConfigurationRepository.getInstance();
		DictionaryRepository.getInstance();
		AnnotatedWordRepository.getInstance();

		logger.debug("Time taken by startup repository:"+(System.currentTimeMillis() - startTime));
		
		String wordQueueReadSQL = "select ID, content from word_queue";
		String wordQueueUpdateSQL = "update word_queue set priority=? where ID =?";

		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			connectionW=DBMW.getInstance().getConnection();
			ps = connectionW.prepareStatement(wordQueueUpdateSQL);
			
			rs = stmt.executeQuery(wordQueueReadSQL);

			TrieRepository trie = TrieRepository.getInstance();
			int count=0;
			while(rs.next())
			{
				Integer ID = rs.getInt("ID");
				String content = rs.getString("content");

				int sublength = trie.searchWordPrefix(content);
				
				
				if(sublength>0)
				{
					long frequency = DictionaryRepository.getInstance().searchFrequency(content);
					if(frequency==0)
						frequency = AnnotatedWordRepository.getInstance().searchFrequency(content);
					
					ps.setInt(1,  (int)frequency);
					ps.setInt(2, ID);
					ps.addBatch();
					count++;
							
					if(count%1000==0)
					{
						ps.executeBatch();
						logger.debug(count+" word priority updated");
					}
					
					
				}				
			}
			rs.close();
			
			if(count%1000!=0)
				ps.executeBatch();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (ps != null) {ps.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
			try{ if (connectionW != null){ DBMW.getInstance().freeConnection(connectionW); connectionW=null;} }catch(Exception ex2){}
		}
		System.out.println("All Prefix Word Priority set.");
		System.out.println("Time taken:"+(System.currentTimeMillis()- startTime )+" milliseconds");
		System.out.flush();
		Thread.sleep(1000);
		System.exit(0);

	}
	
}
