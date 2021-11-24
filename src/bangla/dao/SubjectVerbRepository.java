package bangla.dao;



import org.apache.log4j.Logger;

import bangla.dao.trie.*;
import bangla.grammarchecker.SubVerbRelErrorChecker;

import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubjectVerbRepository implements Repository{
	
	static Logger logger = Logger.getLogger(SubjectVerbRepository.class);
	static SubjectVerbRepository instance = null;
	public static final String tableName="annotated_word";
	
	private SubjectVerbRepository(){
		RepositoryManager.getInstance().addRepository(this);
	}
	
	public static SubjectVerbRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new SubjectVerbRepository();
			}
			
		}
		return instance;
	}
	
	
	public  void insert(long ID, String word) {
		
	
		//ValidateSubVerbRelError.buildSubVerbMap(subVerbMap);		
		return;
	}
	public ArrayList<ArrayList<String>> getSubjectVerbMap(String sql, List<String> columns){
		ArrayList<String> data_DTO ;
		ArrayList<ArrayList<String>> container = new ArrayList<>();
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;

		try{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			//stmt.setQueryTimeout(20);
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				data_DTO = new ArrayList<>();
				data_DTO.add(rs.getString(columns.get(0)));
				data_DTO.add(rs.getString(columns.get(1)));
				if(rs.getString(columns.get(2)) == null) {
					System.out.print("pronoun list can not be null");
					continue;
				}
				data_DTO.add(rs.getString(columns.get(2)));
				//String[] splitted_pro= rs.getString(columns.get(2)).trim().split(",");
				//data_DTO.add(rs.getString(columns.get(2)).trim());//addAll(Arrays.asList(splitted_pro));
				//wordDto.word_type = rs.getInt("service_id");
				//wordDto.lang_type = rs.getInt("service_type");
				//System.out.println("got this DTO: " + word_dto.word);
				container.add(data_DTO);

			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ 
				if (stmt != null) {
					stmt.close();
				}
			} catch (Exception e){}
			
			try{ 
				if (connection != null){ 
					DBMR.getInstance().freeConnection(connection); 
				} 
			}catch(Exception ex2){}
		}
		return container;
	}
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("SubjectVerbRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
	

		String sql = "SELECT cholito, shadhu, subjects ";
		sql += " FROM sub_verb_map " ;
		String[] columns3 = {"cholito", "shadhu", "subjects"};
		List<String> column_ = Arrays.asList(columns3);
		List<ArrayList<String>> subVerbMap = getSubjectVerbMap(sql, column_);
		SubVerbRelErrorChecker.buildSubVerbMap(subVerbMap);
		
		logger.debug("SubjectVerbRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
		
	}
	

	@Override
	public String getTableName()
	{
		// TODO Auto-generated method stub
		
		return tableName;
	}

	@Override
	public void shutDown()
	{
		
	}

}

