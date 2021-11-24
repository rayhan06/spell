package bangla.dto;

import java.util.ArrayList;

public class DocumentDTO 
{
	public String content;
	public char contentChar[];
	public ArrayList<SentenceDTO> sentenceList;
	
	public long logID;
	public int unknownWordCounter;
	public int maxSuggestion;
	public static final Integer LOG_ID_TAG= Integer.MAX_VALUE-1;
	public static final int MAX_SUGGESTION=10;
	
	public DocumentDTO()
	{
		content = null;
		contentChar=null;
		sentenceList = null;
		logID  = -1;
		unknownWordCounter=0;
		maxSuggestion = MAX_SUGGESTION;
	}
	
	public DocumentDTO(String pContent, int pMaxSuggestion)
	{
		content = pContent;
		contentChar=content.toCharArray();
		sentenceList = null;
		logID = -1;
		unknownWordCounter=0;
		maxSuggestion=pMaxSuggestion;
		if(maxSuggestion<1)
			maxSuggestion=MAX_SUGGESTION;
	}
	
	public void addSentence(SentenceDTO pDTO)
	{
		if(sentenceList ==null)
			sentenceList = new ArrayList<SentenceDTO>();
		sentenceList.add(pDTO);
		pDTO.parentDocumentDTO=this;
	}
	
	public void setLogTag(long pLogID)
	{
		logID = pLogID;
	}
	
	public  String toJson() 
	{
		StringBuffer resultantStr = new StringBuffer(10240);
		if(sentenceList==null)return null;
		resultantStr.append('{');
		int index = 0;
		int key=0;
		boolean beforeLogComma=false;
		for(SentenceDTO sentence : sentenceList) 
		{
			if(sentence.wordList!=null)
			for(WordDTO word:sentence.wordList)
			{
				if(word.errorCode>0)
				{
					if( index>0)resultantStr.append(',');
					wordToJson(resultantStr,key++,word);
					index++;
					beforeLogComma=true;
				}
			}
		}
		
		if(beforeLogComma)
			resultantStr.append(',');
		getLogJSON(resultantStr);
				
		resultantStr.append('}');
		return resultantStr.toString();
	}


	public void getSuggestion(StringBuffer resultantStr, ArrayList<WordSuggestionDTO> suggestion) 
	{
		if(suggestion==null)return;
		int index = 0;
		for(WordSuggestionDTO wordSuggestionDTO : suggestion) 
		{
			if(index!=0)resultantStr.append(',');
			resultantStr.append("{\"words\": \"");
			
			resultantStr.append(wordSuggestionDTO.suggestionWord);
			
			if(wordSuggestionDTO.suggestionClass==WordSuggestionDTO.NoSpaceBetweenWordsSuggestionClass&&wordSuggestionDTO.nextSuggestionWordForSpaceError!=null)
			{
				for(String nextWord:wordSuggestionDTO.nextSuggestionWordForSpaceError)
				{
					resultantStr.append(' ');
					resultantStr.append(nextWord);
				}
			}
			
			
			
			resultantStr.append("\",\"score\": ");
			resultantStr.append(wordSuggestionDTO.suggestionClass);
			resultantStr.append('}'); 
			index += 1;
			if(index>=maxSuggestion)break;
		}
	}

	public void wordAndSuggestionToJson(StringBuffer resultantStr, WordDTO wordDTO) 
	{
		resultantStr.append("\"word\": \"");
		resultantStr.append(contentChar, wordDTO.offset, wordDTO.length);
		resultantStr.append('\"');
		
//		resultantStr.append(",\"sequence\": ");
//		resultantStr.append(0); //TODO: check whether this is needed
		resultantStr.append(",\"errorType\": ");
		resultantStr.append(wordDTO.errorCode);
		resultantStr.append(",\"startIndex\": ");
		resultantStr.append(wordDTO.offset);
		resultantStr.append(",\"length\": ");
		resultantStr.append(wordDTO.length);
		if(wordDTO.wordSuggestionList != null)
		{	
//			wordDTO.sortSuggestion();
			resultantStr.append(",\"suggestion\": [");
			getSuggestion(resultantStr, wordDTO.wordSuggestionList);
			resultantStr.append(']');
		}

	}
	
	private  void wordToJson(StringBuffer resultantStr, int key, WordDTO word) 
	{
		resultantStr.append('\"');
		resultantStr.append(key);
		resultantStr.append("\": {");
		wordAndSuggestionToJson(resultantStr, word);
		resultantStr.append('}');
	}
	
	private void getLogJSON(StringBuffer resultantStr)
	{
		resultantStr.append('\"');
		resultantStr.append(LOG_ID_TAG);
		resultantStr.append("\": {\"log_id\": ");
		resultantStr.append(logID);
//		resultantStr.append(",\"startIndex\": ");
//		resultantStr.append(logID);
		resultantStr.append('}');
	}
	
}

