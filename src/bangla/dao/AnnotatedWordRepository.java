package bangla.dao;

import org.apache.log4j.Logger;

import bangla.dao.trie.TrieRepository;
import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class AnnotatedWordRepository implements Repository
{	
	static Logger logger = Logger.getLogger(AnnotatedWordRepository.class);
	static AnnotatedWordRepository instance = null;
	public static final String tableName="annotated_word";

	
	public static AnnotatedWordRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new AnnotatedWordRepository();
			}			
		}
		return instance;
	}

	
	private HashMap<String,Long> annotatedWordToIDMap;
	private HashMap<String,Long> annotatedWordToFrequencyMap;
	private HashMap<Long,String> IDToannotatedWordMap;
	private HashMap<String,ArrayList<Integer>> annotatedWordToPosMap;
	
	private AnnotatedWordRepository()
	{
		annotatedWordToIDMap = new HashMap<String, Long>();
		annotatedWordToFrequencyMap = new HashMap<String, Long>();
		IDToannotatedWordMap = new HashMap<Long,String>();
		annotatedWordToPosMap = new HashMap<String,ArrayList<Integer>>();
		RepositoryManager.getInstance().addRepository(this);
	}
	

	
	
	public void reload(boolean reloadAll) 
	{
		logger.debug("AnnotatedWordRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;
	
		String sql = "select ID, content, class_cat, frequency from annotated_word where isDeleted =0";
		try{
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
					annotatedWordToIDMap.put(content, ID);
					IDToannotatedWordMap.put(ID, content);
					Long oldFrequency = annotatedWordToFrequencyMap.get(content);
					if(oldFrequency!=null)
						frequency = frequency+oldFrequency;
					annotatedWordToFrequencyMap.put(content, frequency);
					
					ArrayList<Integer> posList = annotatedWordToPosMap.get(content);
					if(posList==null)
					{
						posList = new ArrayList<Integer>();					
					}

					posList.add(pos);
					annotatedWordToPosMap.put(content, posList);	
					
					if(frequency>=TrieRepository.MIN_FREQUENCY)TrieRepository.getInstance().insert(content);
				}
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); } }catch(Exception ex2){}
		}

		logger.debug("AnnotatedWordRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");

	}

	public  boolean searchWord(String word) 
	{
		return annotatedWordToIDMap.get(word)!=null;
	}

	public Long getWordID(String word)
	{
		return annotatedWordToIDMap.get(word);
	}

	public String searchWord(Long ID) 
	{
		return IDToannotatedWordMap.get(ID);
	}

	public  long searchFrequency(String word) 
	{
		Long frequency = annotatedWordToFrequencyMap.get(word);
		if(frequency==null)return 0;
		return frequency;
	}
	
	public ArrayList<Integer> getWordPos(String word)
	{
		return annotatedWordToPosMap.get(word);
	}

/*	
	public DictionaryAnnotatedWords root = null;

	private AnnotatedWordRepository(){
		root = new DictionaryAnnotatedWords();
		RepositoryManager.getInstance().addRepository(this);
	}
	
	public  void insert(long ID, String word) {
		
	
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
	public void reload(boolean reloadAll) {
		logger.debug("AnnotatedWordRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;
		GlobalDictionaryRepository repo =  GlobalDictionaryRepository.getInstance();
		String sql = "select ID, content from annotated_word where isDeleted = 0";
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
		
		logger.debug("AnnotatedWordRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");

	}
*/	

	@Override
	public String getTableName() {
		// TODO Auto-generated method stub
		
		return tableName;
	}
	
	@Override
	public void shutDown()
	{
		
	}

}

