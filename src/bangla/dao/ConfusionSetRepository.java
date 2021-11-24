package bangla.dao;

import org.apache.log4j.Logger;

import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.*;

public class ConfusionSetRepository implements Repository
{
	
	static Logger logger = Logger.getLogger(ConfusionSetRepository.class);
	static ConfusionSetRepository instance = null;
	public static final String tableName="real_word_confusion_set";

//	TrieNode root;
	
	public static ConfusionSetRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new ConfusionSetRepository();
			}
			
		}
		return instance;
	}

	
	
	
	private HashMap<String,ArrayList<String>> confusionWordList;
	
	private ConfusionSetRepository()
	{
//		root = new TrieNode();
		confusionWordList = new HashMap<String, ArrayList<String>>();
		RepositoryManager.getInstance().addRepository(this);
	}
	

	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("ConfusionSetRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "select content, confusion_word from real_word_confusion_set where frequency >= 1 and decision not in (4,5)";
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{				
				String confusionWord = rs.getString("content");
				String confusionWordMatch = rs.getString("confusion_word");
				
				ArrayList<String> confusionWordMatchList = confusionWordList.get(confusionWord);
				if(confusionWordMatchList==null)
				{
					confusionWordMatchList = new ArrayList<String>();
					confusionWordList.put(confusionWord, confusionWordMatchList);					
				}
				
				confusionWordMatchList.add(confusionWordMatch);
				
				confusionWordMatchList = confusionWordList.get(confusionWordMatch);
				if(confusionWordMatchList==null)
				{
					confusionWordMatchList = new ArrayList<String>();
					confusionWordList.put(confusionWordMatch, confusionWordMatchList);					
				}
				
				confusionWordMatchList.add(confusionWord);
				
				
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		logger.debug("ConfusionSetRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	}
	
	
	public ArrayList<String> getConfusionSet(String word)
	{
		return confusionWordList.get(word);
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
