package bangla.dao.probability;

import java.sql.*;
import dbm.*;

import org.apache.log4j.*;

import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;

import java.util.HashMap;

public class BigramRepository 
{
	static Logger logger = Logger.getLogger(BigramRepository.class);
	static BigramRepository bigramRepository = null;
	
	HashMap<Long,Integer> bigramMap;
	public static BigramRepository getInstance()
	{
		if(bigramRepository==null)
		{
			createBigramRepository();
		}
		return bigramRepository;
	}
	
	private static synchronized void createBigramRepository()
	{
		if(bigramRepository==null)
		{
			bigramRepository = new BigramRepository();
		}
	}
	
	private BigramRepository()
	{
		bigramMap = new HashMap<Long,Integer>();
	}
	
	public int getFrequency(String one, String two)
	{
		Integer frequency = 0;
		Long oneID = getID(one);
		if(oneID==null) return 0;
		Long twoID = getID(two);
		if(twoID==null) return 0;
		
		
		String sql = "select frequency from word_bigram where firstWord="+oneID+ " and secondWord="+twoID;
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
				frequency = rs.getInt("frequency");				
			}				
			rs.close();
		}catch(Exception ex){
			logger.error("Error in Bigram",ex);
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
//		logger.debug("Frequency:"+frequency);
		return (int)frequency.intValue();
		
	}
	
	public int getFrequency(String one)
	{
		Integer frequency=0;
		Long oneID = getID(one);
		if(oneID==null) return 0;
		
		Integer hashedFrequency = bigramMap.get(oneID);
		if(hashedFrequency!=null)return hashedFrequency;
		
		String sql = "select sum(frequency) as totalFrequency from word_bigram where firstWord="+oneID;
//		logger.debug("Word One:"+one+" AND SQL:"+sql);
		
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
				bigramMap.put(oneID, frequency);
			}				
			rs.close();
		}catch(Exception ex){
			logger.error("Error in Bigram",ex);
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
	//	logger.debug("Frequency:"+frequency);
		
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
