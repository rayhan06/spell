package bangla.dao;



import org.apache.log4j.Logger;

import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.*;

public class NaturalErrorRepository implements Repository
{
	
	static Logger logger = Logger.getLogger(NaturalErrorRepository.class);
	static NaturalErrorRepository instance = null;
//	public DictionaryNaturalError root;
	public static final String tableName="natural_error_word";
	
	private final static int DICTIONARY_WORD = 1;
	private final static int NAMED_ENTITY = 2;
	private final static int ANNOTATED_WORD = 3;

	
	public HashMap<String, ArrayList<String>> errorToCorrect ;
	
	private NaturalErrorRepository(){
		
	//	root = new DictionaryNaturalError();
		errorToCorrect = new HashMap<String, ArrayList<String>>();

		RepositoryManager.getInstance().addRepository(this);
	}
	
	public static NaturalErrorRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new NaturalErrorRepository();
			}
			
		}
		return instance;
	}
	
	public ArrayList<String> getCorrectWord(String errorWord)
	{
		return errorToCorrect.get(errorWord);
	}
/*	
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
	*/
	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("NaturalErrorRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		HashMap<String, ArrayList<String>> localErrorToCorrect = new HashMap<String, ArrayList<String>>();
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;
		
		String sql = "select content, reference_table, reference_id from natural_error_word where reference_table>0 and  isDeleted=0";
		try{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
//				Long ID = rs.getLong("ID");
				String content =rs.getString("content");
				int referenceTable = rs.getInt("reference_table");
				Long referenceID = rs.getLong("reference_id");

				String correctWord=null;
				switch(referenceTable)
				{
				case DICTIONARY_WORD:
					correctWord = DictionaryRepository.getInstance().searchWord(referenceID);
					break;
					
				case ANNOTATED_WORD:
					correctWord = AnnotatedWordRepository.getInstance().searchWord(referenceID);
					break;
				case NAMED_ENTITY:
					correctWord = NamedEntityRepository.getInstance().searchWord(referenceID);
					break;
				}
				
				if(correctWord!=null)
				{
					ArrayList<String> correctWordList = localErrorToCorrect.get(content);
					if(correctWordList==null)
					{
						correctWordList = new ArrayList<String>();
						localErrorToCorrect.put(content, correctWordList);	
					}
					correctWordList.add(correctWord);
					
				}
			}
			errorToCorrect = localErrorToCorrect;
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); } }catch(Exception ex2){}
		}

		logger.debug("NaturalErrorRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");

	}
	

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
