package bangla.dao.probability;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.*;

import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;
import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;

import java.util.HashMap;

public class TrigramRepositoryInMem implements Repository
{
	static Logger logger = Logger.getLogger(TrigramRepositoryInMem.class);
	static TrigramRepositoryInMem trigramRepository = null;
	public static final String tableName="word_trigram";
	
	HashMap<String,Integer> trigramTwoWordSumFrequencyMap;
	HashMap<String,Integer> trigramThreeWordFrequencyMap;
	
	public static TrigramRepositoryInMem getInstance()
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
			trigramRepository = new TrigramRepositoryInMem();
		}
	}
	
	private TrigramRepositoryInMem()
	{
		trigramTwoWordSumFrequencyMap = new HashMap<String,Integer>();
		trigramThreeWordFrequencyMap  = new HashMap<String,Integer>();
		RepositoryManager.getInstance().addRepository(this);
	}
	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("TrigramRepositoryInMem.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "select firstWord, secondWord, thirdWord, frequency from word_trigram WHERE frequency >= 10000";
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				Integer firstWord = rs.getInt("firstWord");
				Integer secondWord = rs.getInt("secondWord");
				Integer thirdWord = rs.getInt("thirdWord");			
				Integer frequency = rs.getInt("frequency");
				
				String firstSecondWordKey = firstWord+";"+secondWord;
				String firstSecondThirdWordKey = firstWord+";"+secondWord+";"+thirdWord;
				trigramThreeWordFrequencyMap.put(firstSecondThirdWordKey, frequency);
				
				Integer twoWordFrequency = trigramTwoWordSumFrequencyMap.get(firstSecondWordKey);
				if(twoWordFrequency==null)
				{
					trigramTwoWordSumFrequencyMap.put(firstSecondWordKey, frequency);
				}
				else
				{
					trigramTwoWordSumFrequencyMap.put(firstSecondWordKey, twoWordFrequency+frequency);
				}
			}
			rs.close();
			
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		logger.debug("TrigramRepositoryInMem.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
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

		String firstSecondThirdWordKey = oneID+";"+twoID+";"+threeID;
		frequency = trigramThreeWordFrequencyMap.get(firstSecondThirdWordKey);
		
		if(frequency==null)return 0;
		return frequency;
	}
	
	public int getFrequency(String one, String two)
	{
		Long oneID = getID(one);
		if(oneID==null) return 0;
		Long twoID = getID(two);
		if(twoID==null) return 0;
		
		String hashKey = oneID+";"+twoID;
		Integer hashedFrequency = trigramTwoWordSumFrequencyMap.get(hashKey);
		if(hashedFrequency==null) return 0;
		
		return hashedFrequency;
	}
	
	public Long getID(String word)
	{
		return BigramRepositoryInMem.getInstance().getID(word);
	}

	@Override
	public String getTableName() 
	{
		return tableName;
	}
	
	@Override
	public void shutDown()
	{
		
	}

}
