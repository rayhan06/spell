package bangla.spellchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.commons.lang3.tuple.Pair;

import bangla.ErrorsInBanglaLanguage;
import bangla.dao.AnnotatedWordRepository;
import bangla.dao.ConfusionSetRepository;
import bangla.dao.DictionaryRepository;
import bangla.dao.MLBasedGeneratedErrorRepository;
import bangla.dao.NamedEntityRepository;
import bangla.dao.NaturalErrorRepository;
import bangla.dao.SuffixPosRepository;
import bangla.dao.trie.*;
import bangla.dto.*;

public class SpellChecker {
	public static Logger logger = Logger.getLogger(SpellChecker.class);

	public static final int MINIMUM_EDIT_DISTANCE_SUGGESTION = 3;
	public static final double REAL_WORLD_THRESHOLD = 0.01;


	public SpellChecker() {
	}

	public void checkForRealWordError(DocumentDTO pDocumentDTO, SentenceDTO sentenceDTO) {
		if (sentenceDTO.wordList == null) return;
		logger.debug("checkForRealWordError called with Sentence:" + sentenceDTO.sentence);

		for (WordDTO wordDTO : sentenceDTO.wordList) {
			String normalized_data = bangla.tokenizer.TextNormalization.normalizeText(wordDTO.word);
			if (normalized_data.equals(wordDTO.word) == false) logger.debug("Normalized data:" + normalized_data);

			ArrayList<String> confusionWordList = ConfusionSetRepository.getInstance().getConfusionSet(normalized_data);

			if (confusionWordList != null) {
				logger.debug("Confusion Word:" + wordDTO.word);
				wordDTO.errorCode = ErrorsInBanglaLanguage.REAL_WORD_ERROR;
				sentenceDTO.wordHaveRealWordError = true;

				wordDTO.addSuggestion(new WordSuggestionDTO(wordDTO.word, WordSuggestionDTO.RealWordErrorSuggestionClass));
				for (String suggestionWord : confusionWordList) {
					wordDTO.addSuggestion(new WordSuggestionDTO(suggestionWord, WordSuggestionDTO.RealWordErrorSuggestionClass));
				}
			}
		}
	}


	public void releaseFalseRealWordError(SentenceDTO pSentenceDTO) {
		for (WordDTO wordDTO : pSentenceDTO.wordList) {
			if (wordDTO.errorCode == ErrorsInBanglaLanguage.REAL_WORD_ERROR) {
				if (wordDTO.wordSuggestionList != null) {
					wordDTO.sortSuggestion();

					int suggestionIndex = 0;
					boolean errorWord = true;
					for (WordSuggestionDTO wordSuggestionDTO : wordDTO.wordSuggestionList) {
						if (wordDTO.word.equals(wordSuggestionDTO.suggestionWord)) {
							break;
						}
						suggestionIndex++;
					}

					if (suggestionIndex == 0) {
						errorWord = false;
						logger.debug("Given word " + wordDTO.word + " probability:" + wordDTO.wordSuggestionList.get(suggestionIndex).statisticalProbability);
						if (wordDTO.wordSuggestionList.size() > 1)
							logger.debug("Next Suggestion" + wordDTO.wordSuggestionList.get(suggestionIndex + 1).suggestionWord + " and Probability: " + wordDTO.wordSuggestionList.get(suggestionIndex + 1).statisticalProbability);
					} else {
						WordSuggestionDTO topSuggestion = wordDTO.wordSuggestionList.get(0);
						WordSuggestionDTO givenWordSuggestion = wordDTO.wordSuggestionList.get(suggestionIndex);

						if (topSuggestion.statisticalProbability - givenWordSuggestion.statisticalProbability < REAL_WORLD_THRESHOLD) {
							errorWord = false;
							logger.debug("Top Suggestion" + topSuggestion.suggestionWord + " and Probability: " + topSuggestion.statisticalProbability);
							logger.debug("Given word " + wordDTO.word + " probability:" + wordDTO.wordSuggestionList.get(suggestionIndex).statisticalProbability);
						}
					}


					if (errorWord == false) {
						wordDTO.errorCode = 0;
						wordDTO.wordSuggestionList = null;
						wordDTO.parentSentenceDTO.wordHaveRealWordError = false;
					}


				}
			}
		}
	}

	public boolean isCorrectWord(String word) {
		if (DictionaryRepository.getInstance().searchWord(word)) {
			logger.debug(word + " found in Dictionary");
			return true;
		} else if (AnnotatedWordRepository.getInstance().searchWord(word)) {
			logger.debug(word + " found in AnnotatedWord");
			return true;
		} else if (NamedEntityRepository.getInstance().searchWord(word)) {
			logger.debug(word + " found in NamedEntity");
			return true;
		}
		
		// need to add code here possibility 2  
		return false;
	}
	
	// finds inflection in word
	public boolean isCorrectInflectedWord(String word){
		int wordLength = word.length();
		ArrayList<String> root_words = new ArrayList<String>();
		
		for(String suffix:SuffixPosRepository.getInstance().getSuffixList()) {
			if(wordLength <= suffix.length())
				continue;
			if(word.substring(wordLength - suffix.length()).equals(suffix)) {
				String root_word = word.substring(0, wordLength - suffix.length());
				
				if(isCorrectWord(root_word)){
					root_words.add(root_word);
					ArrayList<Integer> dictionaryPos = DictionaryRepository.getInstance().getWordPos(root_word);
					ArrayList<Integer> annotatedPos = AnnotatedWordRepository.getInstance().getWordPos(root_word);
					ArrayList<Integer> suffixPos = SuffixPosRepository.getInstance().getSuffixPos(suffix);
					
					if(dictionaryPos != null) {
						dictionaryPos.retainAll(suffixPos);
						if(!dictionaryPos.isEmpty()){
							logger.debug(word + " is valid inflectedWord");
							System.out.println(word + " is valid inflectedWord");
							return true;
						}
					}
					
					if(annotatedPos != null) {
						annotatedPos.retainAll(suffixPos);
						if(!annotatedPos.isEmpty()){
							logger.debug(word + " is valid inflectedWord");
							System.out.println(word + " is valid inflectedWord");
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public void checkWordErrorAndGenSuggestion(DocumentDTO pDocumentDTO, WordDTO wordDTO) {
		logger.debug("checkWordErrorAndGenSuggestion called with Word:" + wordDTO.word);
		String normalized_data = wordDTO.word;

//		if (!(isCorrectWord(normalized_data) || isCorrectInflectedWord(normalized_data))) {
		if (!isCorrectWord(normalized_data)) {
			wordDTO.errorCode = ErrorsInBanglaLanguage.ERROR_WORD;

			logger.debug(normalized_data + " was not found in dictionary, annotated word or named entity");

			logger.debug("Start Searching in error word dictionary");
			int errorScore = WordSuggestionDTO.NaturalErrorSuggestionClass;
			ArrayList<String> suggestedCorrectWordList = NaturalErrorRepository.getInstance().getCorrectWord(normalized_data);

			if (suggestedCorrectWordList == null) {
				suggestedCorrectWordList = MLBasedGeneratedErrorRepository.getInstance().getCorrectWord(normalized_data);
				errorScore = WordSuggestionDTO.MachineLearnedSuggestionClass;
				if (suggestedCorrectWordList != null)
					logger.debug(normalized_data + " found in MLBasedGeneratedErrorRepository");
			}

			if (suggestedCorrectWordList != null) {
				wordDTO.errorCode = ErrorsInBanglaLanguage.ERROR_WORD | ErrorsInBanglaLanguage.WRONG_WORD_ERROR;
				logger.debug(wordDTO.word + " found in natural error/ ML based generated error word");

				for (String suggestedCorrectWord : suggestedCorrectWordList) {
					logger.debug("Error word: " + wordDTO.word + " ,the correct form of error word: " + suggestedCorrectWord);

					wordDTO.addSuggestion(new WordSuggestionDTO(suggestedCorrectWord, errorScore));
				}
			} else {
				// need to add code here possibility 1
				pDocumentDTO.unknownWordCounter++;
				wordDTO.errorCode = ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR;
			}
		}
	}

	public void addCorrectWordSuggestions(DocumentDTO pDocumentDTO, WordDTO wordDTO) {
		int suggestionCount = 0;
		if (wordDTO.wordSuggestionList != null)
			suggestionCount = wordDTO.wordSuggestionList.size();
		for (int i = 1; i < TrieRepository.MAX_EDIT_DISTANCE; i++) {
			logger.debug("Searching for " + i + " Edit Distance");
			TrieRepository.getInstance().getSuggestedWord(wordDTO, i);

			int suggestionCountAfteriThEditDistance = 0;
			if (wordDTO.wordSuggestionList != null)
				suggestionCountAfteriThEditDistance = wordDTO.wordSuggestionList.size();
			logger.debug("Found " + (suggestionCountAfteriThEditDistance - suggestionCount) + " suggestion words.");

			if (suggestionCountAfteriThEditDistance - suggestionCount > MINIMUM_EDIT_DISTANCE_SUGGESTION) break;
		}
	}


	public static void main(String args[]) {
		String words[] = {"আমি", "আমিভাল"};

		for (int j = 0; j < words.length; j++) {

			String word = words[j];

			for (int i = 1; i < word.length(); i++) {
				String firstWord = word.substring(0, i);
				String secondWord = word.substring(i, word.length());
				System.out.println("First Word:" + firstWord + " AND Second Word:" + secondWord);
			}
		}

	}


}