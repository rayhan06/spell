package bangla;


/*
  
 /usr/jdk1.8.0_161/bin/java -Xmx10240m -classpath /usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/DBManager.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/log4j-1.2.17.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/mysql-connector-java-8.0.17.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/classes   bangla.ConfusionSentenceGenerator > ConfusionSentenceGeneration.txt &

 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import dbm.DBMR;
import dbm.DBMW;


public class ConfusionSentenceGenerator 
{
	static Logger logger = Logger.getLogger(ConfusionSentenceGenerator.class);

	public static void main(String args[]) throws Exception
	{
		System.out.println("Starting ConfusionSentenceGenerator ");
		Connection connection = null, connectionW=null;
		Statement stmt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		long startTime = System.currentTimeMillis();
		
		String sentenceReadSQL = "select ID, content from sentence";
		String homonymsReadSQL = "select ID, content from homonyms";
		String confusionSentenceWriteSQL="insert into confusion_sentence values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		String homonymsContent[] = new String [ 5000];
		String homonymsContentWithBothSideSpace[] = new String [ 5000];		
		int homoymsID[] = new int [5000];
		
		int homoymsCount=0;
		
		java.util.HashMap<String, String> homonymsContentIndexMap = new java.util.HashMap<String,String>(); 
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			connectionW=DBMW.getInstance().getConnection();
			ps = connectionW.prepareStatement(confusionSentenceWriteSQL);
			
			rs = stmt.executeQuery(homonymsReadSQL);

			
			while(rs.next())
			{
//				boolean alreadyExists=false;
				
				homoymsID[homoymsCount] = rs.getInt("ID");
				homonymsContent[homoymsCount] = rs.getString("content");
				
				
/*				for( int i=0;i<homoymsCount;i++)
				{
					if(homonymsContent[i].equals(homonymsContent[homoymsCount]))
					{
						alreadyExists = true;
						break;
					}
				}*/
				
				String previousHomonymsContent = homonymsContentIndexMap.get(homonymsContent[homoymsCount]);
				if(previousHomonymsContent!=null)continue;
				homonymsContentIndexMap.put(homonymsContent[homoymsCount],homonymsContent[homoymsCount]);
				
//				if(alreadyExists)continue;
				homonymsContentWithBothSideSpace[homoymsCount] = " "+homonymsContent[homoymsCount]+" ";
				
				homoymsCount++;
			}
			rs.close();
			homonymsContentIndexMap.clear();
			
			logger.debug("homonyms read complete. Total :"+homoymsCount+" Time:"+(System.currentTimeMillis()-startTime) + " ms" );
			rs = stmt.executeQuery(sentenceReadSQL);
			
			int count=0;
			while(rs.next())
			{
				Long sentenceID = rs.getLong("ID");
				String content = rs.getString("content");
				if(content!=null)
				{
					content = " "+content+" ";
					for(int i=0;i<homoymsCount;i++)
					{
						if(content.indexOf(homonymsContentWithBothSideSpace[i])>=0)
						{
							ps.setLong(1, DBMW.getInstance().getNextSequenceId("confusion_sentence"));
							ps.setLong(2,sentenceID);
							ps.setString(3, content);
							ps.setInt(4,homoymsID[i]);
							ps.setString(5,homonymsContent[i]);
							ps.setInt(6, 0);
							
							ps.setInt(7, 0);
							ps.setInt(8, 0);
							ps.setInt(9, 0);
							ps.setInt(10, 0);
							ps.setInt(11, 0);
							

							ps.setInt(12, 0);
							ps.setInt(13, 0);
							ps.setInt(14, 0);
							ps.setInt(15, 0);
							
							ps.addBatch();
							count++;
							
							if(count%10000==0)
							{
								ps.executeBatch();
								logger.debug(count+" confusion sentence inserted");
							}
						}
					}
				}				
			}
			rs.close();
			
			ps.executeBatch();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (ps != null) {ps.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
			try{ if (connectionW != null){ DBMW.getInstance().freeConnection(connectionW); connectionW=null;} }catch(Exception ex2){}
		}
		System.out.println("All Confusion Sentence insertion complete.");
		System.out.println("Time taken:"+(System.currentTimeMillis()- startTime )+" milliseconds");
		System.out.flush();
		Thread.sleep(1000);
		System.exit(0);

	}
	
}
