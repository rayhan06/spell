package bangla;

import config.GlobalConfigConstants;
import config.GlobalConfigDTO;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import bangla.dao.*;
import bangla.dao.probability.*;
import bangla.dao.trie.TrieRepository;
import bangla.grammarchecker.*;
import bangla.spellchecker.*;
import bangla.tokenizer.*;
import config.GlobalConfigurationRepository;
import repository.RepositoryManager;
import bangla.mlproxy.*;
import bangla.dto.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class SpellAndGrammarChecker {

	public static Logger logger = Logger.getLogger(SpellAndGrammarChecker.class);

	SpellChecker spellChecker;
	GrammarChekerModelProxy grammarCheckerModelProxy;

	private static boolean startupCodeStatus = false;

	LinkedBlockingQueue<SentenceDTO> sentenceProcessQueue;
	SentenceWorkerThread sentenceWorkerThread[];

	public Integer lockSentenceCount;
	private static SpellAndGrammarChecker spellAndGrammarChecker = null;


	public static SpellAndGrammarChecker getInstance() {
		if (spellAndGrammarChecker == null)
			createSpellAndGrammarChecker();
		return spellAndGrammarChecker;
	}

	private static synchronized void createSpellAndGrammarChecker() {
		if (spellAndGrammarChecker == null)
			spellAndGrammarChecker = new SpellAndGrammarChecker();
	}


	private SpellAndGrammarChecker() {
		spellChecker = new SpellChecker();
		grammarCheckerModelProxy = new GrammarChekerModelProxy();
		sentenceProcessQueue = new LinkedBlockingQueue<SentenceDTO>();
		lockSentenceCount = new Integer(0);
	}

	public synchronized boolean startupCode() {
		if (startupCodeStatus) return false;
		startupCodeStatus = true;

		String logFilePath = "log4j.properties";

		try {
			logFilePath = getClass().getClassLoader().getResource(logFilePath).toString();
			if (logFilePath.startsWith("file:")) logFilePath = logFilePath.substring(5);
			PropertyConfigurator.configure(logFilePath);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		logger.debug("Loading Repository inside startupCode at " + System.currentTimeMillis());
		RepositoryManager.getInstance();
		GlobalConfigurationRepository.getInstance();
		DictionaryRepository.getInstance();
		AnnotatedWordRepository.getInstance();
		NamedEntityRepository.getInstance();
		NaturalErrorRepository.getInstance();
		MLBasedGeneratedErrorRepository.getInstance();

		ConfusionSetRepository.getInstance();

//		GradedPronoun.getInstance();
//		SubjectVerbRepository.getInstance();
//		SadhuCholitMixture.getInstance();


		GrammerCheckerFactory.initializeRegisteredCheckers();

		logger.debug("Total Trie Node:" + TrieRepository.nodeCount);
		return true;
	}


	public synchronized void checkDocument(DocumentDTO pDocumentDTO) {
		try {
			logger.debug("-------------------------Start of Tokenized Sentences------------------------");

			SentenceTokenizer.sentenceTokenizer(pDocumentDTO);
			if (pDocumentDTO.sentenceList == null) {
				logger.debug("Received 0 tokenized sentence.");
				return;
			}

			for (SentenceDTO sentenceDTO : pDocumentDTO.sentenceList) {
				checkSentence(pDocumentDTO, sentenceDTO);
			}
		} catch (Exception ex) {

			logger.error("Error in Servlet", ex);
		}

	}

	public void checkSentence(DocumentDTO pDocumentDTO, SentenceDTO sentenceDTO) {
		try {
			char wordNormalizedOutput[] = new char[512];
			logger.debug(sentenceDTO.sentence);
			int startIndex = sentenceDTO.offset;
			int i = sentenceDTO.offset;
			int endIndex = sentenceDTO.offset + sentenceDTO.length - 1;
			for (; i < endIndex; i++) {
				char ch = pDocumentDTO.contentChar[i];
				if ((ch < 0x0980 || ch > 0x09E3) && (ch < 0x200A || ch > 0x200E) && (ch != '-')) {
					if (startIndex == i) startIndex++;
					else {
						if (i - startIndex > 1 || pDocumentDTO.contentChar[startIndex] != '-') {
							WordDTO wordDTO = new WordDTO(pDocumentDTO.contentChar, startIndex, i - startIndex, wordNormalizedOutput);
							sentenceDTO.addWord(wordDTO);
							spellChecker.checkWordErrorAndGenSuggestion(pDocumentDTO, wordDTO);
							if (wordDTO.errorCode != 0) sentenceDTO.wordHaveError = true;
							if (wordDTO.errorCode == ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR)
								sentenceDTO.wordHaveUnknownWord = true;
						}
						startIndex = i + 1;
					}
				}
			}

			if (startIndex < i) {
				int lastCharLen = 1;
				char ch = pDocumentDTO.contentChar[endIndex];
				if ((ch < 0x0980 || ch > 0x09E3) && (ch < 0x200A || ch > 0x200E) && (ch != '-'))
					lastCharLen = 0;

				WordDTO wordDTO = new WordDTO(pDocumentDTO.contentChar, startIndex, i - startIndex + lastCharLen, wordNormalizedOutput);
				sentenceDTO.addWord(wordDTO);
				spellChecker.checkWordErrorAndGenSuggestion(pDocumentDTO, wordDTO);
				if (wordDTO.errorCode != 0) sentenceDTO.wordHaveError = true;
				if (wordDTO.errorCode == ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR)
					sentenceDTO.wordHaveUnknownWord = true;
			}

			if (sentenceDTO.wordHaveUnknownWord) {
				List<WordDTO> unknownWords = new ArrayList<WordDTO>();
				for (WordDTO word : sentenceDTO.wordList) {
					if (word.errorCode == ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR) unknownWords.add(word);
				}
				Hashtable<String, Hashtable<Integer, List<String>>> MLResult = grammarCheckerModelProxy.getUnknownWordsInformation(sentenceDTO, unknownWords);

				for (WordDTO u_word : unknownWords) {
					Hashtable<Integer, List<String>> result = MLResult.get(u_word.word);
					if(result == null || result.isEmpty()) {
						// no suggestion means, it's not unknown
						u_word.errorCode = 0;
						continue;
					}

					if (u_word.wordSuggestionList == null)
						u_word.wordSuggestionList = new ArrayList<WordSuggestionDTO>();

					if (result.containsKey(ErrorsInBanglaLanguage.NO_SPACE_PROBLEM)) {
						String string = result.get(ErrorsInBanglaLanguage.NO_SPACE_PROBLEM).get(0);
						String[] subStrings = string.split(" ");
						if (subStrings.length > 0) {
							boolean isCorrect = true;
							for (String subStr : subStrings) {
								if (!spellChecker.isCorrectWord(subStr)) {
									isCorrect = false;
									break;
								}
							}
							if (isCorrect) {
								u_word.wordSuggestionList.add(new WordSuggestionDTO(string, WordSuggestionDTO.NoSpaceBetweenWordsSuggestionClass));
								u_word.errorCode = ErrorsInBanglaLanguage.ERROR_WORD | ErrorsInBanglaLanguage.NO_SPACE_PROBLEM;
							}
						}

					}

					if (result.containsKey(ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR)) {
						for (String string : result.get(ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR)) {
							u_word.wordSuggestionList.add(new WordSuggestionDTO(string, WordSuggestionDTO.EditDistanceBasedSuggestionClass));
							u_word.errorCode = ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR;
						}
					}

					if (u_word.wordSuggestionList.size() == 0) {
						spellChecker.addCorrectWordSuggestions(pDocumentDTO, u_word);
					}
				}

			}

			if (!sentenceDTO.wordHaveError) {
				addRealWordSuggestions(sentenceDTO);
				addGrammarErrorSuggestions(sentenceDTO);
			}
		} catch (Exception ex) {
			logger.error("Error in SentenceChecking", ex);
		}
	}

	private void addGrammarErrorSuggestions(SentenceDTO sentenceDTO) {
		//@TODO:RAKIB ADD LOGIC TO GET GRAMMAR SUGGESTIONS
	}

	private void addRealWordSuggestions(SentenceDTO sentenceDTO) {
		GlobalConfigDTO configDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.REAL_WORD_THRESHOLD_K);
		int realWordThreshold = 20; //configDto == null ? 10 : Integer.parseInt(configDto.value);

		List<WordDTO> confusingWords = new ArrayList<>();
		for (WordDTO word : sentenceDTO.wordList) {
			if (ConfusionSetRepository.getInstance().getConfusionSet(word.word) != null) {
				confusingWords.add(word);
			}
		}

		if (confusingWords.isEmpty()) return;

		Hashtable<Integer, List<String>> suggestions = grammarCheckerModelProxy.getRealWordSuggestions(sentenceDTO, confusingWords, realWordThreshold);

		for(WordDTO word : sentenceDTO.wordList) {
			int wordPos = sentenceDTO.sentence.trim().indexOf(word.word);
			if(suggestions.containsKey(wordPos) && suggestions.get(wordPos) != null) {
				for(String suggestion : suggestions.get(wordPos)) {
					if (word.wordSuggestionList == null) {
						word.wordSuggestionList = new ArrayList<>();
					}

					word.errorCode = ErrorsInBanglaLanguage.ERROR_WORD | ErrorsInBanglaLanguage.REAL_WORD_ERROR;
					word.wordSuggestionList.add(new WordSuggestionDTO(suggestion, WordSuggestionDTO.RealWordErrorSuggestionClass));
				}
			}
		}
	}

	public void calculateBigramTrigramProbability(SentenceDTO pSentenceDTO) {
		long startTime = System.currentTimeMillis();
		WordDTO firstWord = null, secondWord = null;


		BigramRepositoryInMem bigram = BigramRepositoryInMem.getInstance();        //		BigramRepository bigram = BigramRepository.getInstance();

		TrigramRepositoryInMem trigram = TrigramRepositoryInMem.getInstance();    //		TrigramRepository trigram = TrigramRepository.getInstance();

		int count = 0;
		for (WordDTO wordDTO : pSentenceDTO.wordList) {
			count++;
			if (secondWord != null && secondWord.errorCode == 0) {
				if (wordDTO.wordSuggestionList != null) {
					int secondWordFrequency = bigram.getFrequency(secondWord.word);
					int firstWordSecondWordFrequency = 0;
					if (firstWord != null && firstWord.errorCode == 0) {
						firstWordSecondWordFrequency = trigram.getFrequency(firstWord.word, secondWord.word);
					}

					if (secondWordFrequency > 0) {
						for (WordSuggestionDTO suggestionDTO : wordDTO.wordSuggestionList) {
							suggestionDTO.bigramProbability = bigram.getFrequency(secondWord.word, suggestionDTO.suggestionWord);
							suggestionDTO.bigramProbability = suggestionDTO.bigramProbability / secondWordFrequency;//BigramRepository.getInstance().getFrequency(secondWord.word);

							if (firstWordSecondWordFrequency > 0)//if(firstWord!=null&&firstWord.errorCode==0)
							{
								suggestionDTO.trigramProbability = trigram.getFrequency(firstWord.word, secondWord.word, suggestionDTO.suggestionWord);
								suggestionDTO.trigramProbability = suggestionDTO.trigramProbability / firstWordSecondWordFrequency;//TrigramRepository.getInstance().getFrequency(firstWord.word, secondWord.word);
							}
						}
					}
				}

			}


			if (firstWord != null && firstWord.wordSuggestionList != null) {
				int secondWordFrequency = bigram.getFrequency(secondWord.word);
				int secondThirdWordFrequency = trigram.getFrequency(secondWord.word, wordDTO.word);
				for (WordSuggestionDTO suggestionDTO : firstWord.wordSuggestionList) {
					if (secondWordFrequency > 0) {
						suggestionDTO.backBigramProbability = bigram.getFrequency(suggestionDTO.suggestionWord, secondWord.word);
						suggestionDTO.backBigramProbability = suggestionDTO.backBigramProbability / secondWordFrequency;//BigramRepository.getInstance().getFrequency(secondWord.word);

						if (secondThirdWordFrequency > 0) {
							suggestionDTO.backTrigramProbability = trigram.getFrequency(suggestionDTO.suggestionWord, secondWord.word, wordDTO.word);
							suggestionDTO.backTrigramProbability = suggestionDTO.backTrigramProbability / secondThirdWordFrequency;//TrigramRepository.getInstance().getFrequency(secondWord.word, wordDTO.word);
						}
					}
				}
			}

			if (secondWord != null && count == pSentenceDTO.wordList.size()) {
				logger.debug("Generating Special backBigram for " + secondWord.word);
				if (secondWord.wordSuggestionList != null) {
					int wordDTOFrequency = bigram.getFrequency(wordDTO.word);
					for (WordSuggestionDTO suggestionDTO : secondWord.wordSuggestionList) {
						if (wordDTOFrequency > 0) {
							suggestionDTO.backBigramProbability = bigram.getFrequency(suggestionDTO.suggestionWord, wordDTO.word);
							suggestionDTO.backBigramProbability = suggestionDTO.backBigramProbability / wordDTOFrequency;//BigramRepository.getInstance().getFrequency(wordDTO.word);
						}
					}

				}

			}


			firstWord = secondWord;
			secondWord = wordDTO;


		}

		logger.debug("Probability Calculation for:" + pSentenceDTO.sentence + " , Time:" + (System.currentTimeMillis() - startTime) + " ms");
		for (WordDTO wordDTO : pSentenceDTO.wordList) {
			if (wordDTO.errorCode != 0) {
				logger.debug("Error Word:" + wordDTO.word);
				if (wordDTO.wordSuggestionList != null) {
					for (WordSuggestionDTO suggestionDTO : wordDTO.wordSuggestionList) {
						logger.debug("Suggestion Word:" + suggestionDTO.suggestionWord + " , Bigram:" + suggestionDTO.bigramProbability + " , Trigram:" + suggestionDTO.trigramProbability + " , RBigram:" + suggestionDTO.backBigramProbability + " , RTrigram:" + suggestionDTO.backTrigramProbability);
					}
				}
			}
		}


	}

	public void shutdown() {
		startupCodeStatus = false;
	}
}
