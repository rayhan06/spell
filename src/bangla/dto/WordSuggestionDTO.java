package bangla.dto;

import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;
import java.util.ArrayList;

public class WordSuggestionDTO 
{
	public static final int NaturalErrorSuggestionClass=0;
	public static final int SubjectVerbAgreementClass=5;	
	public static final int SadhuCholitMixClass=6;
	public static final int MachineLearnedSuggestionClass=10;
	public static final int NoSpaceBetweenWordsSuggestionClass=20;

	public static final int EditDistanceBasedSuggestionClass=30;
	
	public static final int RealWordErrorSuggestionClass=20;
	

	public static final int TrigramMultiplier=10000;
	public static final int BigramMultiplier =2500;

	public static final int BackTrigramMultiplier=10000;
	public static final int BackBigramMultiplier =2500;
	
	public String suggestionWord;
	public int suggestionClass;
	public long frequency;
	public int editDistance;
	
	public double bigramProbability, trigramProbability, backBigramProbability, backTrigramProbability, statisticalProbability;
	
	public ArrayList<String> nextSuggestionWordForSpaceError;
	
	public WordSuggestionDTO()
	{
		suggestionWord=null;
		suggestionClass=-1;
		frequency=0;
		editDistance=0;
		backBigramProbability= backTrigramProbability=bigramProbability= trigramProbability=0;
	}
	
	public WordSuggestionDTO(String pWord, int pClass)
	{
		suggestionWord=pWord;
		suggestionClass=pClass;
		frequency = DictionaryRepository.getInstance().searchFrequency(suggestionWord);
		if(frequency==0)
			frequency = AnnotatedWordRepository.getInstance().searchFrequency(suggestionWord);
		editDistance=0;
		
		backBigramProbability= backTrigramProbability=bigramProbability= trigramProbability=0;
	}
	
	public WordSuggestionDTO(String pWord, int pScore, long pFrequency)
	{
		suggestionWord=pWord;
		suggestionClass=pScore;
		frequency = pFrequency;
		
		backBigramProbability= backTrigramProbability=bigramProbability= trigramProbability=0;
	}	

	public WordSuggestionDTO(String pWord, int pScore, int pEditDistance)
	{
		suggestionWord=pWord;
		suggestionClass=pScore;
		frequency = DictionaryRepository.getInstance().searchFrequency(suggestionWord);
		if(frequency==0)
			frequency = AnnotatedWordRepository.getInstance().searchFrequency(suggestionWord);
		editDistance=pEditDistance;
		
		backBigramProbability= backTrigramProbability=bigramProbability= trigramProbability=0;
	}	

	public void calculateStatisticalProbability()
	{
		statisticalProbability = TrigramMultiplier* trigramProbability+BigramMultiplier*bigramProbability
				+BackBigramMultiplier* backBigramProbability+BackTrigramMultiplier* backTrigramProbability;
	}
	
	public void addNextSuggestionWordForSpaceError(String suggestionWord, int pFrequency)
	{
		if(nextSuggestionWordForSpaceError==null)
			nextSuggestionWordForSpaceError = new ArrayList<String>();
		nextSuggestionWordForSpaceError.add(suggestionWord);
		frequency+=pFrequency;
	}
}
