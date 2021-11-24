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

public class GradedPronoun implements Repository{
	
	static Logger logger = Logger.getLogger(GradedPronoun.class);
	static GradedPronoun instance = null;
	
	public static final String tableName="annotated_word";
	
	private GradedPronoun(){
	
		RepositoryManager.getInstance().addRepository(this);
	}
	
	public static GradedPronoun getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new GradedPronoun();
			}
			
		}
		return instance;
	}
	
	
	public  void insert(long ID, String word) {
		
	
		//ValidateSubVerbRelError.buildSubVerbMap(subVerbMap);		
		return;
	}
//	public List<spell_checking_dto> getSpellCheckingDTOs(String sql, List<String> columns){
//		List<spell_checking_dto> data_DTO = new ArrayList<>();
//		Connection connection = null;
//		ResultSet rs = null;
//		Statement stmt = null;
//		spell_checking_dto  sp_dto= null;
//		try{
//			//System.out.println(sql);
//			//logger.debug("sql " + sql);
//			//Class.forName("com.mysql.jdbc.Driver");
//			connection = DBMR.getInstance().getConnection();
//			stmt = connection.createStatement();
//			stmt.setQueryTimeout(20);
//			rs = stmt.executeQuery(sql);
//			while(rs.next()){
//				sp_dto = new spell_checking_dto();
//				sp_dto.word = rs.getString(columns.get(0));// column names should be explicit
//				sp_dto.wordType = rs.getString(columns.get(1));
//				//wordDto.word_type = rs.getInt("service_id");
//				//wordDto.lang_type = rs.getInt("service_type");
//				//System.out.println("got this DTO: " + word_dto.word);
//				data_DTO.add(sp_dto);
//
//			}				
//		}catch(Exception ex){
//			ex.printStackTrace();
//		}finally{
//			try{ 
//				if (stmt != null) {
//					stmt.close();
//				}
//			} catch (Exception e){}
//			
//			try{ 
//				if (connection != null){ 
//					DBMR.getInstance().freeConnection(connection); 
//				} 
//			}catch(Exception ex2){}
//		}
//		return data_DTO;
//	}
	public List<GrammarDto> getPurusInfo(String sql, List<String> columns){
		List<GrammarDto> data_DTO = new ArrayList<>();
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;
		GrammarDto  sp_dto= null;
		try{
			//System.out.println(sql);
			//logger.debug("sql " + sql);
			//Class.forName("com.mysql.jdbc.Driver");
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			stmt.setQueryTimeout(20);
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				sp_dto = new GrammarDto();
				sp_dto.pronoun = rs.getString(columns.get(0));// column names should be explicit
				sp_dto.marker = String.valueOf(rs.getInt(columns.get(1)));
				//wordDto.word_type = rs.getInt("service_id");
				//wordDto.lang_type = rs.getInt("service_type");
				//System.out.println("got this DTO: " + word_dto.word);
				data_DTO.add(sp_dto);

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
		return data_DTO;
	}
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("GradedPronoun.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();


		String sql = "SELECT pronoun, marker ";
		sql += " FROM purus " ;
		String[] columns2 = {"pronoun", "marker"};
		List<String> column_ = Arrays.asList(columns2);
		List<GrammarDto> purus = getPurusInfo(sql, column_);
		SubVerbRelErrorChecker.setPurushMap(purus);
		
		logger.debug("GradedPronoun.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	
		
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
