package bangla.dao;

import org.apache.log4j.Logger;

import com.itextpdf.text.pdf.PdfStructTreeController.returnType;

import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.*;

public class SuffixPosRepository implements Repository
{
	
	static Logger logger = Logger.getLogger(SuffixPosRepository.class);
	static SuffixPosRepository instance = null;
	public static final String tableName="suffix_pos";
	
	
	public static SuffixPosRepository getInstance(){
		if (instance == null){
			synchronized (tableName) {
				if(instance == null)
					instance = new SuffixPosRepository();
			}
			
		}
		return instance;
	}

	private HashMap<String,ArrayList<Integer>> suffixPosList;

	
	private  SuffixPosRepository() {
		suffixPosList = new HashMap<String, ArrayList<Integer>>();
		RepositoryManager.getInstance().addRepository(this);
	}
	
	
	@Override
	public void reload(boolean reloadAll) 
	{
		logger.debug("SuffixPosRepository.reload("+reloadAll+") Started");
		long startTime = System.currentTimeMillis();
		
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "select suffix, pos from suffix_pos where isDeleted=0";
		
		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{				
				String suffix = rs.getString("suffix");
				int pos = rs.getInt("pos");
				
				ArrayList<Integer> posList = suffixPosList.get(suffix);
				if(posList==null)
				{
					posList = new ArrayList<Integer>();					
				}

				posList.add(pos);
				suffixPosList.put(suffix, posList);					
				
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		logger.debug("SuffixPosRepository.reload("+reloadAll+") Ended in "+(System.currentTimeMillis()-startTime) +" milli-seconds");
	}
	
	
	public ArrayList<Integer> getSuffixPos(String suffix)
	{
		return suffixPosList.get(suffix);
	}
	
	public Set<String> getSuffixList(){
		return suffixPosList.keySet();
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
