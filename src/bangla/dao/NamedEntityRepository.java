package bangla.dao;

import org.apache.log4j.Logger;
import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.*;

public class NamedEntityRepository implements Repository
{
	
	static Logger logger = Logger.getLogger(NamedEntityRepository.class);
	static NamedEntityRepository instance = null;
	public static final String tableName="named_entity";

	
	
	public static NamedEntityRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new NamedEntityRepository();
			}
			
		}
		return instance;
	}

	
	
	private HashMap<String,Long> namedEntityToIDMap;
	private HashMap<Long,String> IDToNamedEntityMap;
	private HashMap<String, Integer>namedEntityToTypeMap;
	private ArrayList<String> suffixList;
	private ArrayList<Integer> suffixTypeList;
	
	private NamedEntityRepository()
	{
		namedEntityToIDMap = new HashMap<String,Long>();
		IDToNamedEntityMap = new HashMap<Long,String>();
		namedEntityToTypeMap = new HashMap<String,Integer>();
		suffixList = new ArrayList<String>();
		suffixTypeList = new ArrayList<Integer>();
		RepositoryManager.getInstance().addRepository(this);
	}
	

	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("NamedEntityRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "select ID, type_cat, content from named_entity where isDeleted=0";
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				Long ID = rs.getLong("ID");
				Integer type = rs.getInt("type_cat");
				String content = rs.getString("content");
				if(content!=null)
				{
					namedEntityToIDMap.put(content,  ID);
					IDToNamedEntityMap.put(ID, content);
					namedEntityToTypeMap.put(content, type);
				}
			}		
			rs.close();
			
			suffixTypeList.clear();
			suffixList.clear();
			sql = "select type_cat, content from word_suffix_ne where isDeleted=0";
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				Integer type = rs.getInt("type_cat");
				String suffix = rs.getString("content");
				
				boolean uniqueSuffix=true;
				for(String suffixWord:suffixList)
				{
					if(suffixWord.equals(suffix))
					{
						uniqueSuffix=false;
						break;
					}
				}
				
				if(uniqueSuffix)
				{
					suffixTypeList.add(type);
					suffixList.add(suffix);
				}
			}		
			rs.close();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		logger.debug("NamedEntityRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	}
	
	public  boolean searchWord(String word) 
	{
		Long ID = namedEntityToIDMap.get(word);
		if(ID!=null)return true;
		
	
		for(int i=0;i<suffixList.size();i++)
		{
			String suffix = suffixList.get(i);
			Integer suffixType = suffixTypeList.get(i);
			
			if(word.endsWith(suffix))
			{
				String rootWord = word.substring(0,word.length()-suffix.length());
				
				logger.debug("NamedEntity:"+word+" Root:"+rootWord+" Suffix:"+suffix);
				ID = namedEntityToIDMap.get(rootWord);
				
				if(ID!=null)
					{
					Integer type = namedEntityToTypeMap.get(rootWord);
					if(suffixType==type)
						return true;
					}
			}
		}
		
		
		
		return false;
		
	}

	public  String searchWord(Long ID) 
	{
		return IDToNamedEntityMap.get(ID);
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
