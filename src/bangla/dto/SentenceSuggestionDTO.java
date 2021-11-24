package bangla.dto;

public class SentenceSuggestionDTO 
{
	public static final int NO_CLASS=0;
	
	public String suggestionStr;
	int suggestionClass;
	int score;
	
	public SentenceSuggestionDTO()
	{
		suggestionStr = null;
		suggestionClass= NO_CLASS;
		score=-1;
	}
	
	public SentenceSuggestionDTO(String pSuggestionStr, int pSuggestionClass, int pScore)
	{
		suggestionStr = pSuggestionStr;
		suggestionClass = pSuggestionClass;
		score=pScore;
	}
	
}
