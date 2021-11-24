package bangla.dao.probability;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.*;

import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;
import dbm.DBMR;

import java.util.HashMap;

public class TrigramRepository 
{
	static Logger logger = Logger.getLogger(TrigramRepository.class);
	static TrigramRepository trigramRepository = null;
	
	HashMap<String,Integer> trigramMap;
	public static TrigramRepository getInstance()
	{
		if(trigramRepository==null)
		{
			createTrigramRepository();
		}
		return trigramRepository;
	}
	
	private static synchronized void createTrigramRepository()
	{
		if(trigramRepository==null)
		{
			trigramRepository = new TrigramRepository();
		}
	}
	
	private TrigramRepository()
	{
		trigramMap = new HashMap<String,Integer>();
	}
	
	
	public int getFrequency(String one, String two, String three)
	{
		Integer frequency = 0;
		Long oneID = getID(one);
		if(oneID==null) return 0;
		Long twoID = getID(two);
		if(twoID==null) return 0;
		Long threeID = getID(three);
		if(threeID==null)return 0;
		
		String sql = "select frequency from word_trigram where firstWord="+oneID+ " and secondWord="+twoID+ " and thirdWord="+threeID;
//		logger.debug("Word One:"+one+", Word Two:"+two+", Word Three:"+three+" AND SQL:"+sql);
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				frequency = rs.getInt("frequency");				
			}				
			rs.close();
		}catch(Exception ex){
			logger.error("Error in Trigram",ex);
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
	//	logger.debug("Frequency:"+frequency);
		return frequency;
		
	}
	
	public int getFrequency(String one, String two)
	{
		Integer frequency = 0;
		Long oneID = getID(one);
		if(oneID==null) return 0;
		Long twoID = getID(two);
		if(twoID==null) return 0;
		
		String hashKey = oneID+":"+twoID;
		Integer hashedFrequency = trigramMap.get(hashKey);
		if(hashedFrequency!=null)return hashedFrequency;
		
		String sql = "select sum(frequency) as totalFrequency from word_trigram where firstWord="+oneID+ " and secondWord="+twoID;
//		logger.debug("Word One:"+one+", Word Two:"+two+" AND SQL:"+sql);
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				frequency = rs.getInt("totalFrequency");
				trigramMap.put(hashKey, frequency);
				
			}				
			rs.close();
		}catch(Exception ex){
			logger.error("Error in Trigram",ex);
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
//		logger.debug("Frequency:"+frequency);
		
		return frequency;
		
	}
	
	public Long getID(String word)
	{
		Long wordID = DictionaryRepository.getInstance().getWordID(word);
		if(wordID==null)
			wordID = AnnotatedWordRepository.getInstance().getWordID(word);
		
		return wordID;
	}
	
}
