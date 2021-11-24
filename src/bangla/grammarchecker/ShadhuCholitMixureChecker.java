package bangla.grammarchecker;

import java.util.List;

import org.apache.log4j.Logger;


import bangla.ErrorsInBanglaLanguage;
import bangla.dao.GrammarDto;
import bangla.withtrie.TrieNodeSearchResult;
import bangla.withtrie.TrieNodeWithList;
import bangla.tokenizer.WordTokenizer;
import bangla.dto.*;

public class ShadhuCholitMixureChecker implements BanglaGrammarChecker 
{
	static Logger logger = Logger.getLogger(ShadhuCholitMixureChecker.class);
	static TrieNodeWithList cholitToShadhu = new TrieNodeWithList();
	static TrieNodeWithList shadhuToCholit = new TrieNodeWithList();
	static WordTokenizer WT = new WordTokenizer();

	public static void buildTrie(List<GrammarDto> words) 
	{

		for (GrammarDto mixed : words) 
		{
			if (mixed == null)
				continue;
			boolean isValidSadhu = (mixed.shadhu != null && !mixed.shadhu.equals("invalid"));
			boolean isValidCholito = (mixed.cholit != null && !mixed.cholit.equals("invalid"));
			if (isValidSadhu && isValidCholito) {
				cholitToShadhu.insert(mixed.cholit, mixed.shadhu);
				shadhuToCholit.insert(mixed.shadhu, mixed.cholit);
			}
		}
	}

	public int hasError(SentenceDTO sentenceDTO) 
	{
		logger.debug("hasError called with : "+sentenceDTO.parentDocumentDTO.content.substring(sentenceDTO.offset,sentenceDTO.offset+sentenceDTO.length));		
		int cholitCount = 0;
		int sadhuCount = 0;


		for (WordDTO wordDTO : sentenceDTO.wordList) 
		{
			TrieNodeSearchResult res = cholitToShadhu.searchWord(wordDTO.word);
// 	If corresponding sadhu word is correct then we are adding it to suggestions
			if (res.isFound && res.viceVersaWord != null && !res.viceVersaWord.equals("")
					&& !res.viceVersaWord.equals("NotFound") && !res.viceVersaWord.equals("invalid")
					&& !wordDTO.word.equals(res.viceVersaWord)) 
			{
				cholitCount++;
				logger.debug("Cholit Word:"+wordDTO.word);
				continue;
			}

			res = shadhuToCholit.searchWord(wordDTO.word);
// 	If corresponding cholito word is correct then we are adding it to suggestions
			if (res.isFound && res.viceVersaWord != null && !res.viceVersaWord.equals("")
					&& !res.viceVersaWord.equals("NotFound") && !res.viceVersaWord.equals("invalid")
					&& !wordDTO.word.equals(res.viceVersaWord)) 
			{
				sadhuCount++;
				logger.debug("Sadhu Word:"+wordDTO.word);				
				continue;
			}
		}

		logger.debug("Found Sadhu Word:" + sadhuCount +" and Cholit Word:"+cholitCount);
		if (cholitCount > 0 && sadhuCount > 0) 
		{ // initially &&
			if(cholitCount >= sadhuCount) 
			{
				for (WordDTO wordDTO : sentenceDTO.wordList) 
				{
					TrieNodeSearchResult res = cholitToShadhu.searchWord(wordDTO.word);
//		 	If corresponding sadhu word is correct then we are adding it to suggestions
					if (res.isFound && res.viceVersaWord != null && !res.viceVersaWord.equals("")
							&& !res.viceVersaWord.equals("NotFound") && !res.viceVersaWord.equals("invalid")
							&& !wordDTO.word.equals(res.viceVersaWord)) 
					{
						wordDTO.errorCode = wordDTO.errorCode |  ErrorsInBanglaLanguage.SHADHU_CHOLITO_MIXING_ERROR;
						wordDTO.addSuggestion(new WordSuggestionDTO(res.viceVersaWord, WordSuggestionDTO.SadhuCholitMixClass ));
					}
				}

			}
			else
			{
				for (WordDTO wordDTO : sentenceDTO.wordList) 
				{
					TrieNodeSearchResult res = shadhuToCholit.searchWord(wordDTO.word);
//		 	If corresponding cholito word is correct then we are adding it to suggestions
					if (res.isFound && res.viceVersaWord != null && !res.viceVersaWord.equals("")
							&& !res.viceVersaWord.equals("NotFound") && !res.viceVersaWord.equals("invalid")
							&& !wordDTO.word.equals(res.viceVersaWord)) 
					{
						wordDTO.errorCode = wordDTO.errorCode |  ErrorsInBanglaLanguage.SHADHU_CHOLITO_MIXING_ERROR;
						wordDTO.addSuggestion(new WordSuggestionDTO(res.viceVersaWord, WordSuggestionDTO.SadhuCholitMixClass ));
					}

				}
				
			}
			return ErrorsInBanglaLanguage.SHADHU_CHOLITO_MIXING_ERROR;
		}
		
		return 0;
	}
}