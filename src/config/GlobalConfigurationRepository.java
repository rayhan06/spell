package config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;


import dbm.*;
import repository.Repository;
import repository.RepositoryManager;

public class GlobalConfigurationRepository implements Repository
{
	private static Logger logger = Logger.getLogger(GlobalConfigurationRepository.class);
	private static GlobalConfigurationRepository instance = null;
	public static final String tableName="global_config";
	


	private static HashMap<Integer, GlobalConfigDTO> GlobalConfigDTOByID;
	private static HashMap<Integer, HashMap<Integer, GlobalConfigDTO>> GlobalConfigDTOByGroupID;

	
	private GlobalConfigurationRepository()
	{
		GlobalConfigDTOByID = new HashMap<Integer, GlobalConfigDTO>();
		GlobalConfigDTOByGroupID = new HashMap<>();
		
		RepositoryManager.getInstance().addRepository(this);
	}
	
	public static GlobalConfigurationRepository getInstance()
	{
		if(instance == null)
		{
			createInstance();
		}
		return instance;
	}
	private synchronized static void createInstance()
	{
		if(instance == null)
		{
			instance = new GlobalConfigurationRepository();
		}
		
	}
	public static synchronized Collection<GlobalConfigDTO> getConfigsByGroupID(int groupID)
	{		
		getInstance();
		return GlobalConfigDTOByGroupID.get(groupID).values();
	}
	public static synchronized Collection<GlobalConfigDTO> getAllConfigs()
	{		
		getInstance();
		return GlobalConfigDTOByID.values();
	}	
	public static synchronized GlobalConfigDTO getGlobalConfigDTOByID(int configID)
	{
		getInstance();
		if(GlobalConfigDTOByID.get(configID) == null)
		{
			GlobalConfigDTO globalConfigDTO = new GlobalConfigDTO();
			return globalConfigDTO;
		}
		return GlobalConfigDTOByID.get(configID);
	}
	
	public void reload(boolean reloadAll)
	{
		Connection connection = null;
		Statement statement = null;
		String sql = null;
		ResultSet resultSet = null;
		try{
			connection = DBMR.getInstance().getConnection();
			statement = connection.createStatement();
			sql = "select * from global_config";
			resultSet = statement.executeQuery(sql);
			while(resultSet.next())
			{
				GlobalConfigDTO globalConfigDTO = new GlobalConfigDTO();
				globalConfigDTO.ID = resultSet.getInt("ID");
				globalConfigDTO.name = resultSet.getString("name");
				globalConfigDTO.value = resultSet.getString("value");
				globalConfigDTO.comments = resultSet.getString("comments");
				globalConfigDTO.groupID = resultSet.getInt("groupID");
					GlobalConfigDTOByID.put(globalConfigDTO.ID, globalConfigDTO);
				HashMap<Integer, GlobalConfigDTO> list = GlobalConfigDTOByGroupID.get(globalConfigDTO.groupID); 
				if(list == null)
				{
					list = new HashMap<Integer, GlobalConfigDTO>();
				}
				list.put(globalConfigDTO.ID, globalConfigDTO);
				GlobalConfigDTOByGroupID.put(globalConfigDTO.groupID, list);
			}
		}
		catch(Exception ex)
		{
			logger.fatal("GlobalConfiguration object: ",ex);
		}
		finally {
			try {if(statement != null){statement.close();}}catch(Exception e) {}			
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); }}catch(Exception e) {}			
		}		
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
