package bangla.dao;


import org.apache.log4j.Logger;

import dbm.DBMR;
import repository.Repository;
import repository.RepositoryManager;
import java.sql.*;
import java.util.*;

public class MLBasedGeneratedErrorRepository implements Repository {

	static Logger logger = Logger.getLogger(MLBasedGeneratedErrorRepository.class);
	static MLBasedGeneratedErrorRepository instance = null;

	public static final String tableName = "unique_error_injected_word_ml";

	public HashMap<String, ArrayList<String>> errorToCorrect;

	private MLBasedGeneratedErrorRepository() {

		errorToCorrect = new HashMap<String, ArrayList<String>>();

		RepositoryManager.getInstance().addRepository(this);
	}

	public static MLBasedGeneratedErrorRepository getInstance() {
		if (instance == null) {
			synchronized (tableName) {
				if (instance == null)
					instance = new MLBasedGeneratedErrorRepository();
			}

		}
		return instance;
	}

	public ArrayList<String> getCorrectWord(String errorWord) {
		return errorToCorrect.get(errorWord);
	}

	@Override
	public void reload(boolean reloadAll) {
		logger.debug("MLBasedGeneratedErrorRepository.reload(" + reloadAll + ") Started");
		long startTime = System.currentTimeMillis();

		HashMap<String, ArrayList<String>> localErrorToCorrect = new HashMap<String, ArrayList<String>>();
		Connection connection = null;
		ResultSet rs = null;
		Statement stmt = null;

		String sql = "select original_word, error_word from unique_error_injected_word_ml WHERE decision = 1 OR decision = 2";
		try {
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String correctWord = rs.getString("original_word");
				String errorWord = rs.getString("error_word");

				if (correctWord.startsWith("-") == false) {
					ArrayList<String> correctWordList = localErrorToCorrect.get(errorWord);
					if (correctWordList == null) {
						correctWordList = new ArrayList<String>();
						localErrorToCorrect.put(errorWord, correctWordList);
					}
					correctWordList.add(correctWord);

				}
			}
			errorToCorrect = localErrorToCorrect;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (Exception e) {
			}
			try {
				if (connection != null) {
					DBMR.getInstance().freeConnection(connection);
				}
			} catch (Exception ex2) {
			}
		}

		logger.debug("MLBasedGeneratedErrorRepository.reload(" + reloadAll + ") Ended in " + (System.currentTimeMillis() - startTime) + " milli-seconds");

	}


	@Override
	public String getTableName() {

		return tableName;
	}

	@Override
	public void shutDown() {

	}

}
