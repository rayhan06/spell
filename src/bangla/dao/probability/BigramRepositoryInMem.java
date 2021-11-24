package bangla.dao.probability;

import java.sql.*;
import dbm.*;
import repository.Repository;
import repository.RepositoryManager;

import org.apache.log4j.*;

import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;


import java.util.HashMap;

public class BigramRepositoryInMem  implements Repository
{
	static Logger logger = Logger.getLogger(BigramRepositoryInMem.class);
	static BigramRepositoryInMem bigramRepository = null;
	public static final String tableName="word_bigram";
	
	HashMap<Integer,Integer> bigramMapSumFreqOneWord;
	HashMap<String,Integer>bigramMapFreqTwoWord;
	
	public HashMap<String,Long>wordIDMap;
	
	public static BigramRepositoryInMem getInstance()
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
			bigramRepository = new BigramRepositoryInMem();
		}
	}
	
	private BigramRepositoryInMem()
	{
		bigramMapSumFreqOneWord = new HashMap<Integer,Integer>();
		bigramMapFreqTwoWord = new HashMap<String,Integer>();
		wordIDMap = new HashMap<String,Long>();	
		RepositoryManager.getInstance().addRepository(this);
	}

	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("BigramRepositoryInMem.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "select firstWord, secondWord, frequency from word_bigram where frequency >= 10000";
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			
			

			rs = stmt.executeQuery("select ID, content, frequency from dictionary_words where isDeleted=0");
				
			while(rs.next())
			{
				Long ID = rs.getLong("ID");
				String content = rs.getString("content");
				wordIDMap.put(content, ID);
			}
			rs.close();
				
			rs = stmt.executeQuery( "select ID, content, frequency from annotated_word where isDeleted =0");
			while(rs.next())
			{
				Long ID = rs.getLong("ID");
				String content = rs.getString("content");
				wordIDMap.put(content, ID);
			}
			rs.close();
				
			logger.debug("Word-ID Map loading done");	
			
			
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				Integer firstWord = rs.getInt("firstWord");
				Integer secondWord = rs.getInt("secondWord");				
				Integer frequency = rs.getInt("frequency");
				
				String key = firstWord+";"+secondWord;
				bigramMapFreqTwoWord.put(key, frequency);
				
				Integer oneWordFrequency = bigramMapSumFreqOneWord.get(firstWord);
				if(oneWordFrequency==null)
				{
					bigramMapSumFreqOneWord.put(firstWord, frequency);
				}
				else
				{
					bigramMapSumFreqOneWord.put(firstWord, oneWordFrequency+frequency);
				}
			}
			rs.close();
			
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		logger.debug("BigramRepositoryInMem.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	}
	
	
	public int getFrequency(String one, String two)
	{
		Long oneID = getID(one);
		if(oneID==null) return 0;
		Long twoID = getID(two);
		if(twoID==null) return 0;
		
		String key = oneID+";"+twoID;
		Integer frequency = bigramMapFreqTwoWord.get(key);
		
		if(frequency ==null)return 0;
		return (int)frequency.intValue();		
	}
	
	public int getFrequency(String one)
	{
		Long oneID = getID(one);
		if(oneID==null) return 0;
		
		Integer hashedFrequency = bigramMapSumFreqOneWord.get(oneID.intValue());
		if(hashedFrequency==null)return 0;
		
		
		return hashedFrequency;		
	}
	
	public Long getID(String word)
	{
		return wordIDMap.get(word);
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
