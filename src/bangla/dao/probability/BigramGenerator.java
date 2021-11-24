package bangla.dao.probability;


/*
  
 /usr/jdk1.8.0_161/bin/java -Xms81920m -classpath /usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/DBManager.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/log4j-1.2.17.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/lib/mysql-connector-java-8.0.17.jar:/usr/local/jakarta-tomcat-9.0.17_live_project/webapps/spell_checking_api/WEB-INF/classes   bangla.dao.probability.BigramGenerator > BigramGeneration.txt &

 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import bangla.SpellAndGrammarChecker;
import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;
import bangla.tokenizer.WordTokenizer;
import config.GlobalConfigurationRepository;

import java.util.*;
import java.util.Map.Entry;

import dbm.DBMR;
import dbm.DBMW;
import repository.RepositoryManager;

class BigramDTO
{
	long ID;
	String firstWord, secondWord;
	Long firstWordID, secondWordID;
	int frequency;
}

public class BigramGenerator 
{
	static Logger logger = Logger.getLogger(BigramGenerator.class);

	public static void main(String args[]) throws Exception
	{
		System.out.println("Starting BigramGenerator ");
		
		long bigramCount=0;
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		long startTime = System.currentTimeMillis();
		HashMap<String,BigramDTO> bigramMap = new HashMap<String,BigramDTO>(50000000);
		HashMap<String,Long>wordIDMap = new HashMap<String,Long>();
		//String sql = "select ID, content from sentence where isDeleted=0";
		String sql[] = {"select content from sentence","select content from sentence_1","select content from sentence_2",
				"select content from sentence_3","select content from sentence_4","select content from sentence_5"};


		try
		{
			connection = DBMR.getInstance().getConnection();
			stmt = connection.createStatement();
			
			
			rs = stmt.executeQuery("select ID, content, frequency from dictionary_words where isDeleted=0");
			
			while(rs.next())
			{
				Long ID = rs.getLong("ID");
				String content = rs.getString("content");
				wordIDMap.put(content, ID);
			}
			rs.close();
			
			rs = stmt.executeQuery( "select ID, content, frequency from annotated_word where isDeleted =0");
			while(rs.next())
			{
				Long ID = rs.getLong("ID");
				String content = rs.getString("content");
				wordIDMap.put(content, ID);
			}
			rs.close();
			
			
			System.out.println("Word-ID map loading done");
			
			for( int sqli=0;sqli<sql.length;sqli++)
			{
				
			System.out.println("Going to execute : "+sql[sqli]);

			rs = stmt.executeQuery(sql[sqli]);
			
			while(rs.next())
			{
				String content = rs.getString("content");
				if(content!=null)
				{
					String firstWord=null, secondWord=null;

					
					char contentChar[] = content.toCharArray();
//					char wordNormalizedOutput[] = new char[512];
					int startIndex=0;
					int i=0;
					int endIndex = content.length()-1; 
					for(;i<endIndex;i++)
					{
						char ch = contentChar[i]; 
						if(ch=='-')
						{
							if(startIndex==i)startIndex++;
						}
						else if( (ch<0x0980||ch>0x09E3) && (ch<0x200A||ch>0x200E) )
						{
							if(startIndex==i)startIndex++;
							else
							{
								if(i-startIndex>1)
								{
									int wordLength = i-startIndex;
									while(contentChar[startIndex+wordLength-1]=='-')
									{
										wordLength--;
										if(wordLength<=0)break;
									}
									
									if (wordLength>0)
									{
										secondWord = new String(contentChar, startIndex, wordLength);
										bigramCount = processWord(firstWord, secondWord, bigramMap,bigramCount, wordIDMap);			
										firstWord = secondWord;
									}
								}
								startIndex = i+1;
							}
						}
					}
							
					if(startIndex<i)
					{						
						int lastCharLen=0;
						char ch = contentChar[endIndex]; 
						
						if(ch=='-')
						{
							while(ch=='-')
							{
								lastCharLen--;
								ch=contentChar[endIndex+lastCharLen ];
							}
						}
						else if( (ch<0x0980||ch>0x09E3) && (ch<0x200A||ch>0x200E) )
							lastCharLen=0;						
						else 
							lastCharLen=1;
						
						if(i-startIndex+lastCharLen>0)
						{
							secondWord = new String(contentChar, startIndex, i-startIndex+lastCharLen);
							bigramCount = processWord(firstWord, secondWord, bigramMap, bigramCount, wordIDMap);			
							firstWord = secondWord;
						}
					}							

				}
				
//				if(trigramCount > 1000000)break;
			}
			rs.close();
			rs=null;
			
				if(sqli+1==sql.length/2 || sqli==sql.length-1)
				{

					ArrayList<String> removeList = new ArrayList<String>();
			
					for(Entry<String, BigramDTO> entry:bigramMap.entrySet())
					{
						String key = entry.getKey();
						BigramDTO dto = entry.getValue();
						if(dto.frequency<=1)
						{
							removeList.add(key);
						}
					}
					System.out.println("Removing "+removeList.size()+" Bigrams ");
			
					for(String key:removeList)
					{
						bigramMap.remove(key);
					}
					removeList.clear();
					removeList=null;

					System.out.println("Going to call GarbageCollection");
					long time = System.currentTimeMillis();
					System.gc();
					System.out.println("GarbageCollection executed in:"+(System.currentTimeMillis()-time)+" milliseconds");
				}
			
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (stmt != null) {stmt.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMR.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
		}
		
		System.out.println("All Bigram generation complete. Total:"+bigramCount);
		System.out.println("Time taken:"+(System.currentTimeMillis()- startTime )+" milliseconds");
		
		startTime = System.currentTimeMillis();
		
		PreparedStatement ps =null;
		
		try
		{
			String insertSql = "insert into word_bigram values (?,?,?,?)";
		
			connection = DBMW.getInstance().getConnection();
			ps = connection.prepareStatement(insertSql);
			System.out.println("Starting Bigram insertion...");
			int count=0;
			for(Entry<String, BigramDTO> entry:bigramMap.entrySet())
			{
				BigramDTO dto = entry.getValue();
				if(dto.frequency<=4)continue;
				ps.setLong(1, DBMW.getInstance().getNextSequenceId("word_bigram"));
				ps.setInt(2, dto.firstWordID.intValue());
				ps.setInt(3, dto.secondWordID.intValue());
				ps.setInt(4, dto.frequency);
				ps.addBatch();
				count++;
				if(count%100000==0)
				{
					System.out.println("Going to insert:"+count/1000000.0+ " million");
					ps.executeBatch();
				}
			}			
			if(count%100000!=0)
			{
				System.out.println("Going to insert last:"+count%100000);
				ps.executeBatch();
				System.out.println("All insertion complete. Total Insertion:"+count);
			}
			
						
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{ if (ps != null) {ps.close();}} catch (Exception e){}
			try{ if (connection != null){ DBMW.getInstance().freeConnection(connection); connection=null;} }catch(Exception ex2){}
			
		}

		System.out.println("All Bigram insertion complete.");
		System.out.println("Time taken:"+(System.currentTimeMillis()- startTime )+" milliseconds");

		Thread.sleep(100);
		System.exit(0);

	}
	
	
	public static long processWord(String firstWord, String secondWord,HashMap<String,BigramDTO> bigramMap , long bigramCount, 
			HashMap<String,Long>wordIDMap)
	{
//		for(String secondWord:wordTokenList)
		{
			if(firstWord!=null)
			{
				StringBuffer keyBuf = new StringBuffer(200);
				keyBuf.append(firstWord);
				keyBuf.append(';');
				keyBuf.append(secondWord);
				
				String key = keyBuf.toString();
				BigramDTO dto = bigramMap.get(key);
				if(dto==null)
				{
					dto = new BigramDTO();
					dto.firstWord = firstWord;
					dto.firstWordID = wordIDMap.get(firstWord);
					if(dto.firstWordID!=null)
					{
						dto.secondWord=secondWord;
						dto.secondWordID = wordIDMap.get(secondWord);
						if(dto.secondWordID!=null)
						{
								dto.frequency=1;
								bigramMap.put(key, dto);
								bigramCount++;
								if(bigramCount%1000000==0)
									System.out.println("Bigram Generation:"+(bigramCount/1000000) +" million");
								
								
						
						}
					}
				}
				
				else dto.frequency++;
			}

			firstWord = secondWord;
		}
		return bigramCount;
	}
}
