package bangla.dto;

import java.util.ArrayList;

public class SentenceDTO 
{
	public String sentence;
	public int offset, length;

	public boolean wordHaveUnknownWord, wordHaveError, wordHaveRealWordError, isCached;
	public ArrayList<WordDTO> wordList;
	WordDTO headWordDTO;

	public int errorCode;
	public ArrayList<SentenceSuggestionDTO> suggestedSentenceList;
	
	public DocumentDTO parentDocumentDTO;
	
	public SentenceDTO()
	{
		sentence=null;
		offset = -1;
		length=0;
		errorCode= 0;
		wordHaveError=wordHaveRealWordError=isCached=false;
		wordList=null;
		suggestedSentenceList = null;
		headWordDTO=null;
	}
	
	public SentenceDTO(String pSentence, int pOffset, int pLength)
	{
		sentence=pSentence;
		offset = pOffset;
		length= pLength;
		errorCode= 0;
		wordHaveError=wordHaveRealWordError=isCached=false;
		wordList=null;
		suggestedSentenceList = null;
		headWordDTO=null;
	}
	
	public void addWord(WordDTO pDTO)
	{
		WordDTO prevWordDTO=null;
		if(wordList ==null)
		{
			wordList = new ArrayList<WordDTO>();
			headWordDTO=pDTO;
		}
		else
		{
			prevWordDTO = wordList.get(wordList.size()-1);
			prevWordDTO.next=pDTO;
			pDTO.prev=prevWordDTO;
		}
		
		wordList.add(pDTO);
		pDTO.parentSentenceDTO=this;
	}
}
