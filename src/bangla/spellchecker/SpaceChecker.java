package bangla.spellchecker;

import org.apache.log4j.Logger;

import bangla.ErrorsInBanglaLanguage;
import bangla.dao.AnnotatedWordRepository;
import bangla.dao.DictionaryRepository;
import bangla.dao.probability.BigramRepositoryInMem;
import bangla.dto.*;

public class SpaceChecker {
	static Logger logger = Logger.getLogger(SpaceChecker.class);
	public static final int MIN_WORD_FREQUENCY = 0;
	public static char[] replaceableBySpace = {'-', ':', 'ঃ'};
	public static String startCharList = "অআইঈউঊঋঌএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহড়ঢ়য়";

	public boolean isReplaceableCharBySpace(char ch) {
		for (int i = 0; i < replaceableBySpace.length; i++)
			if (replaceableBySpace[i] == ch)
				return true;
		return false;
	}

	public boolean isBanglaWordStartChar(char ch) {
		return startCharList.indexOf(ch) >= 0;
	}


	public int check(DocumentDTO pDocumentDTO, WordDTO wordDTO) {
		//If any word ends with "ঃ" , it is replaced by "space" : 					
		if (wordDTO.word.endsWith("ঃ")) {
			wordDTO.addSuggestion(new WordSuggestionDTO(wordDTO.word.substring(0, wordDTO.word.length() - 1), WordSuggestionDTO.NaturalErrorSuggestionClass));
		}
		int result = addSpaceBetweenWordSuggestion(pDocumentDTO, wordDTO, wordDTO.word, null, 0);

		if (wordDTO.wordSuggestionList == null) return result;


		return result;

	}


	public int checkForAdditionalSpace(WordDTO wordDTO) {
		logger.debug("Checking Additional Space for:" + wordDTO.word);

		if (wordDTO.prev == null) return 0;

		if (wordDTO.errorCode == 0 && wordDTO.prev.errorCode == 0)
			return 0;

		if (wordDTO.errorCode == ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR || wordDTO.prev.errorCode == ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR) {

			WordDTO prevWordDTO = wordDTO.prev;


			String newConnectedWord = prevWordDTO.word + wordDTO.word;

			Long newWordID = DictionaryRepository.getInstance().getWordID(newConnectedWord);
			if (newWordID == null) newWordID = AnnotatedWordRepository.getInstance().getWordID(newConnectedWord);
			if (newWordID == null) return 0;

			int bigramFrequency = 0, rBigramFrequency = 0;
			if (prevWordDTO.prev != null) {
				bigramFrequency = BigramRepositoryInMem.getInstance().getFrequency(prevWordDTO.prev.word, newConnectedWord);
				logger.debug("Prev Prev Word:" + prevWordDTO.prev.word + " , bigramF:" + bigramFrequency);
			}

			logger.debug("Joined Word:" + newConnectedWord);
			if (wordDTO.next != null) {
				rBigramFrequency = BigramRepositoryInMem.getInstance().getFrequency(newConnectedWord, wordDTO.next.word);

				logger.debug("Next Word:" + wordDTO.next.word + " , rBiigramF:" + rBigramFrequency);
			}

			if (bigramFrequency + rBigramFrequency >= MIN_WORD_FREQUENCY) {
				prevWordDTO.next = wordDTO.next;
				if (wordDTO.next != null) {
					wordDTO.next.prev = prevWordDTO;
				}

				prevWordDTO.length = wordDTO.length + wordDTO.offset - prevWordDTO.offset;

				if (prevWordDTO.wordSuggestionList != null) prevWordDTO.wordSuggestionList.clear();
				prevWordDTO.addSuggestion(new WordSuggestionDTO(newConnectedWord, WordSuggestionDTO.NoSpaceBetweenWordsSuggestionClass, bigramFrequency + rBigramFrequency));
				prevWordDTO.errorCode = ErrorsInBanglaLanguage.ERROR_WORD | ErrorsInBanglaLanguage.EXTAR_SPACE_ERROR;

				wordDTO.prev = wordDTO.next = null;
				wordDTO.errorCode = -1;
				return 1;
			}
		}

		return 0;
	}

	public int addSpaceBetweenWordSuggestion(DocumentDTO pDocumentDTO, WordDTO wordDTO, String word, WordSuggestionDTO wordSuggestionDTO, int recursionLevel) {
		if (recursionLevel > 10) return 0;
		logger.debug("addSpaceBetweenWordSuggestion started with:" + word);
		int count = 0;

		try {
			String firstWord[] = new String[word.length()];
			String secondWord[] = new String[word.length()];
			boolean firstCorrectSecondIncorrectTag[] = new boolean[word.length()];

			for (int i = 1; i < word.length() - 1; i++) {
				char ithChar = word.charAt(i);

				if (isBanglaWordStartChar(ithChar) == false)
					continue;

				firstWord[i] = word.substring(0, i);

				if (isReplaceableCharBySpace(ithChar)) {
					secondWord[i] = word.substring(i + 1, word.length());
					logger.debug("Replaceable by Space:" + ithChar);
				} else
					secondWord[i] = word.substring(i, word.length());

				firstCorrectSecondIncorrectTag[i] = false;

				if (DictionaryRepository.getInstance().searchWord(firstWord[i]) || AnnotatedWordRepository.getInstance().searchWord(firstWord[i])) {
					if (DictionaryRepository.getInstance().searchWord(secondWord[i]) || AnnotatedWordRepository.getInstance().searchWord(secondWord[i])) {
						long firstFrequency = DictionaryRepository.getInstance().searchFrequency(firstWord[i]);
						if (firstFrequency == 0)
							firstFrequency = AnnotatedWordRepository.getInstance().searchFrequency(firstWord[i]);
						long secondFrequency = DictionaryRepository.getInstance().searchFrequency(secondWord[i]);
						if (secondFrequency == 0)
							secondFrequency = AnnotatedWordRepository.getInstance().searchFrequency(secondWord[i]);

						if (recursionLevel == 0) {
							wordSuggestionDTO = new WordSuggestionDTO(firstWord[i], WordSuggestionDTO.NoSpaceBetweenWordsSuggestionClass, firstFrequency);
							wordSuggestionDTO.addNextSuggestionWordForSpaceError(secondWord[i], (int) secondFrequency);
							wordDTO.addSuggestion(wordSuggestionDTO);
						} else {
							wordSuggestionDTO.addNextSuggestionWordForSpaceError(firstWord[i], (int) firstFrequency);
							wordSuggestionDTO.addNextSuggestionWordForSpaceError(secondWord[i], (int) secondFrequency);

						}


						StringBuffer debugStr = new StringBuffer("Adding Suggestion:");
						debugStr.append(wordSuggestionDTO.suggestionWord);

						for (String suggestionWord : wordSuggestionDTO.nextSuggestionWordForSpaceError) {
							debugStr.append(' ');
							debugStr.append(suggestionWord);
						}
						debugStr.append(" With frequency:");
						debugStr.append(wordSuggestionDTO.frequency);
						logger.debug(debugStr);


						count++;
					} else
						firstCorrectSecondIncorrectTag[i] = true;

				}
			}

			if (count == 0) {
				for (int i = 1; i < word.length(); i++) {
					if (firstCorrectSecondIncorrectTag[i] && secondWord[i].length() > 2) {
						long firstFrequency = DictionaryRepository.getInstance().searchFrequency(firstWord[i]);
						if (firstFrequency == 0)
							firstFrequency = AnnotatedWordRepository.getInstance().searchFrequency(firstWord[i]);
						wordSuggestionDTO = new WordSuggestionDTO(firstWord[i], WordSuggestionDTO.NoSpaceBetweenWordsSuggestionClass, firstFrequency);

						int suggestionCount = addSpaceBetweenWordSuggestion(pDocumentDTO, wordDTO, secondWord[i], wordSuggestionDTO, recursionLevel + 1);

						if (suggestionCount > 0) {
							wordDTO.addSuggestion(wordSuggestionDTO);
							count += suggestionCount;
						}
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Error in addSpaceBetweenWordSuggestion", ex);
		}

		if (recursionLevel == 0)            //TODO:Sort Suggestions
		{
		}

		return count;
	}

}
