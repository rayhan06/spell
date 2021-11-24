package bangla.dao;

import org.apache.log4j.Logger;

import bangla.dao.trie.TrieRepository;
import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.*;
public class DictionaryRepository implements Repository
{
	
	static Logger logger = Logger.getLogger(DictionaryRepository.class);
	static DictionaryRepository instance = null;
	public static final String tableName="dictionary_words";

	
	public static DictionaryRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new DictionaryRepository();
			}
			
		}
		return instance;
	}

	
	
	
	private HashMap<String,Long> dictionaryStringToIDMap;
	private HashMap<String,Long> dictionaryStringToFrequencyMap;
	private HashMap<Long,String> dictionaryIDtoStringMap;
	private HashMap<String,ArrayList<Integer>> dictionaryStringToPosMap;
	
	
	private DictionaryRepository()
	{
		dictionaryStringToIDMap = new HashMap<String,Long>();
		dictionaryStringToFrequencyMap = new HashMap<String,Long>();
		dictionaryIDtoStringMap = new HashMap<Long,String>();
		dictionaryStringToPosMap = new HashMap<String,ArrayList<Integer>>();
		RepositoryManager.getInstance().addRepository(this);
	}
	

	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("DictionaryRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "select ID, content, class_cat, frequency from dictionary_words where isDeleted=0";
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				Long ID = rs.getLong("ID");
				String content = rs.getString("content");
				int pos = rs.getInt("class_cat");
				Long frequency = rs.getLong("frequency");
				if(content!=null)
				{
					if(content.startsWith("-")==false)
					{
						
						if(dictionaryStringToIDMap.get(content)==null)
						{
						dictionaryIDtoStringMap.put(ID, content);
						dictionaryStringToIDMap.put(content, ID);
						dictionaryStringToFrequencyMap.put(content, frequency);
						}
						else
						{
							Long oldFrequency = dictionaryStringToFrequencyMap.get(content);
							if(oldFrequency!=null)
								dictionaryStringToFrequencyMap.put(content, frequency+oldFrequency);
						}
						
						ArrayList<Integer> posList = dictionaryStringToPosMap.get(content);
						if(posList==null)
						{
							posList = new ArrayList<Integer>();					
						}

						posList.add(pos);
						dictionaryStringToPosMap.put(content, posList);	
						
						if(frequency>=TrieRepository.MIN_FREQUENCY)TrieRepository.getInstance().insert(content);
					}
				}
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		logger.debug("DictionaryRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	}
	
	public  boolean searchWord(String word) 
	{
		return dictionaryStringToIDMap.get(word)!=null;
	}
	
	public Long getWordID(String word)
	{
		return dictionaryStringToIDMap.get(word);
	}
	
	public  String searchWord(Long ID) 
	{
		return dictionaryIDtoStringMap.get(ID);
	}
	
	public  long searchFrequency(String content) 
	{
		Long frequency = dictionaryStringToFrequencyMap.get(content);
		if(frequency==null) return 0;
		return frequency;
	}
	
	public ArrayList<Integer> getWordPos(String word)
	{
		return dictionaryStringToPosMap.get(word);
	}
	
/*	
	public DictionaryCorrectWords root = null;
	private DictionaryRepository()
	{
		root = new DictionaryCorrectWords();
		RepositoryManager.getInstance().addRepository(this);
	}
	
	public  void insert(long ID, String word) 
	{
		TrieNodeWithList recurseRoot = root.dict;
		for(int i=0;i<word.length();i++) {
			char c = word.charAt(i);
			TrieNodeWithList temp=null;
			boolean isFound = false;
			for(TrieNodeWithList node: recurseRoot.children) {
				if(node.c==c) {
					temp = node;
					isFound = true;
					break;
				}
			}
			if(!isFound) {
				temp = new TrieNodeWithList(c);
				temp.parent = recurseRoot;
				temp.isWord= false;
				recurseRoot.children.add(temp);
			}
			if(i==word.length() - 1) {
				temp.isWord = true;
				this.root.inverseDict.put(ID, word);
			
			}
			recurseRoot = temp;
		}
		return;
	}
	
	public  boolean searchWord(String word) {
		List<TrieNodeWithList> child = root.dict.children;
		for(int i=0;i<word.length();i++) {
			char c = word.charAt(i);
			if(c=='\u0000')
				return false;
			TrieNodeWithList temp=null;
			boolean isFound = false;
			try {
				for(TrieNodeWithList node: child) {
					if(node.c==c) {
						temp=node;
						isFound = true;
						break;
					}
				}
			}catch(Exception ex) 
			{
				logger.fatal(ex);
			}
			if(!isFound) return false;
			child=temp.children;
			if(i==word.length()-1) {
				if(temp.isWord) {
					return true;
				}
			}
		}
		return false;
	}

	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("DictionaryRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;
		GlobalDictionaryRepository repo =  GlobalDictionaryRepository.getInstance();
		String sql = "select ID, content from dictionary_words where isDeleted=0";
		try{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				this.insert(rs.getLong("ID"), rs.getString("content"));
				repo.addToGlobalDictionary(rs.getString("content"), rs.getInt("ID"));

			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); } }catch(Exception ex2){}
		}
		NoSpaceBetweenWordsChecker.registerDictionary(root.dict);
		SpaceErrorBetweenWordsChecker.registerDictionary(root.dict);
		NirdeshokErrorChecker.registerDictionary(root.dict);
		
		logger.debug("DictionaryRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	}
	*/

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
