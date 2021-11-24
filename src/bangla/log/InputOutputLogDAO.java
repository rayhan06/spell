package bangla.log;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

import dbm.*;

public class InputOutputLogDAO extends Thread
{
	public class LogDTO
	{
		public long ID;
		public long ip;
		public String inputText;
		public String response;
		public String sessionID;
		public int clientCat;
		public int unknownWordCount;
		
		public LogDTO(long pID, long pIP,  String pinputText, String presponse, String psessionID, int pclientCat, int punknownWordCount)
		{
			ID = pID;
			ip = pIP;
			inputText = pinputText;
			response = presponse;
			sessionID = psessionID;
			clientCat = pclientCat;
			unknownWordCount = punknownWordCount;
		}
	}
	
	Logger logger = Logger.getLogger(InputOutputLogDAO.class);
	String tableName = "spell_checker_log";
	
	LinkedBlockingQueue<LogDTO> logList;
	boolean running;
	
	public static InputOutputLogDAO inputOutputLogDAO=null;
	
	
	public static InputOutputLogDAO getInstance()
	{
		if(inputOutputLogDAO==null)
		{
			createInputOutputDAO();
		}
		return inputOutputLogDAO;
	}
	
	private static synchronized void createInputOutputDAO()
	{
		if(inputOutputLogDAO==null)
		{
			inputOutputLogDAO = new InputOutputLogDAO();
		}
		
	}
	private InputOutputLogDAO()
	{
		logList = new LinkedBlockingQueue<LogDTO>();
		running = true;
		start();
	}
	
	public long getNextLogID()
	{
		try
		{
		return DBMW.getInstance().getNextSequenceId(tableName);
		}catch(Exception ex)
		{
			logger.error("Failed to get nextID for Log table",ex);
		}
		
		return -1;
	}
	
	public long add(long ID, String ipAddressStr,  String inputText, String response, String sessionID, int clientCat, int unknownWordCount)
	{
		try
		{
			byte requestIPByte[] = InetAddress.getByName(ipAddressStr).getAddress();
			long ip = requestIPByte[0]&0xFF;
			ip = ip<<8 |(requestIPByte[1]&0xFF);
			ip = ip<<8 |(requestIPByte[2]&0xFF);
			ip = ip<<8 |(requestIPByte[3]&0xFF);		

			logger.debug("Pushing Log with ID:"+ID+ " at :"+System.currentTimeMillis());
			LogDTO dto = new LogDTO(ID, ip, inputText,response, sessionID, clientCat,unknownWordCount);
			logList.offer(dto);
		}
		catch(Exception ex)
		{
			logger.error("Exception in converting IP", ex);
		}
		return ID;
	}
	
	public void run()
	{
		Connection connection = null;
		PreparedStatement ps = null;

		while(running)
		{
			try
			{
				LogDTO dto = logList.take();
				if(dto.ID==-1)continue;
				
				long lastModificationTime = System.currentTimeMillis();	
				logger.debug("Inserting Log with ID:"+dto.ID+" at :"+lastModificationTime);
		
	
				String sql = "INSERT INTO " + tableName+ " (ID, content, response, unknown_word_count, client_cat, ip, time, session, isDeleted, lastModificationTime)"
						+" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

				connection = DBMW.getInstance().getConnection();
				ps = connection.prepareStatement(sql);

				int index = 1;

				ps.setObject(index++,dto.ID);
				ps.setObject(index++,dto.inputText);
				ps.setObject(index++,dto.response);
				ps.setObject(index++,dto.unknownWordCount);
				ps.setObject(index++,dto.clientCat);
				ps.setObject(index++,dto.ip);
				ps.setObject(index++,lastModificationTime);
				ps.setObject(index++,dto.sessionID);
				ps.setObject(index++,0);
				ps.setObject(index++, lastModificationTime);
				ps.executeUpdate();

				logger.debug(dto.ID+" Log inserted at :"+System.currentTimeMillis());
			}catch(Exception ex)
			{
				logger.error(ex);
			}finally{
				try{if (ps != null) {ps.close();ps=null;}} catch(Exception e){}
				try{if(connection != null){DBMW.getInstance().freeConnection(connection);connection=null;}}catch(Exception ex2){}
			}
		}	
		logger.debug("Exiting InputOutputLogDAO");
	}
							
	public void shutdown()
	{
		if(running)
		{
			try
			{
				running = false;
				logList.put(new LogDTO(-1, -1, null, null, null, -1,-1));
			}catch(Exception ex)
			{
				logger.error("Error while shutdown",ex);
			}
		}
	}
}
	