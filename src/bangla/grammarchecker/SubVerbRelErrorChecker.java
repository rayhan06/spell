package bangla.grammarchecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import bangla.ErrorsInBanglaLanguage;
import bangla.dao.GrammarDto;
import bangla.withtrie.TrieNodeWithList;
import bangla.tokenizer.WordTokenizer;

import bangla.dto.*;

public class SubVerbRelErrorChecker implements BanglaGrammarChecker 
{
	private static Logger logger = Logger.getLogger(SubVerbRelErrorChecker.class);
	private static final String notFound = "NotFound";
	private static final List<String> organismSubjects = Arrays.asList("সে", "এরা", "ও", "এই");
	private static final List<String> uttomPurush = Arrays.asList("আমি", "আমরা", "আমাকে", "আমার", "আমাদের", "মোর","মোরা");
	private static final List<String> priority1 = Arrays.asList("তোরা", "উনারা", "তাহারা", "আমি", "আমরা", "মোরা",
			"তুমি", "তোমরা", "তোমাদের", "সে", "তারা", "আপনি", "আপনারা", "তিনি", "তিঁনি", "তারা", "ইনি", "এরা", "উনি",	"ওরা");

	static Map<String, String> purushMap;
	private static final String NI = "নি";
	private static final String NA = "না";
	static WordTokenizer WT = new WordTokenizer();
	static Map<String, Integer> verbIndex = new HashMap<>();
	static Map<String, Integer> sadhuCholitMap = new HashMap<>();
	static List<List<String>> subVerbMatrix = new ArrayList<>();
	static TrieNodeWithList verbList = new TrieNodeWithList();
	static TrieNodeWithList namedEntity = new TrieNodeWithList();

	public int hasError(SentenceDTO sentenceDTO) 
	{
		logger.debug("hasError called with : "+sentenceDTO.parentDocumentDTO.content.substring(sentenceDTO.offset,sentenceDTO.offset+sentenceDTO.length));
		//List<SpellCheckingDto> spellCheckerDtos = new ArrayList<>();
		int namedEntityType = 0;
		if (sentenceDTO.wordList.size() >= 5) 
		{
			//return getDtosWithoutError(words.size());
			logger.debug("Sentence have more than MAX_WORD_LIMIT for Subject-verb:"+sentenceDTO.wordList.size());
			return 0;
		}
		
		WordDTO subjectWordDTO = findSubject(sentenceDTO);
		if (subjectWordDTO==null) 
		{
			subjectWordDTO = findSubjectFromNamedEntity(sentenceDTO);
			if (subjectWordDTO!=null)
				namedEntityType = getNamedEntityCategory(subjectWordDTO.word);
		}
		
		
		WordDTO verbWordDTO = findVerb(sentenceDTO);

		if (subjectWordDTO==null || verbWordDTO==null) 
		{
			//return getDtosWithoutError(words.size());
			return 0;
		}
		
		logger.debug("Found Subject : "+subjectWordDTO.word+ " and Verb : "+verbWordDTO.word);
		
		if (!isSubjectVerbConflict(subjectWordDTO.word, verbWordDTO.word, namedEntityType)) 
		{
			logger.debug("Subject and Verb do not conflict");
			//return getDtosWithoutError(words.size());
			return 0;
		} 
		else 
		{
			logger.debug("Subject and Verb are conflicting.");
			subjectWordDTO.errorCode = ErrorsInBanglaLanguage.SUB_VERB_AGREEMENT_ERROR;
			List<String> alternativ_subjects = getSuggestedSubject(verbWordDTO.word);
			int indicator = 0;
			
			if (sadhuCholitMap.containsKey(verbWordDTO.word)) 
			{
				indicator = sadhuCholitMap.get(verbWordDTO.word);
			}
			
			if (alternativ_subjects != null) 
			{
				for (String each_subjects : alternativ_subjects) 
				{
					int v = 0;
					if (purushMap.containsKey(each_subjects)) 
					{
						v = Integer.valueOf(purushMap.get(each_subjects));
					}
					if (v == 0 || v == indicator) 
					{
						subjectWordDTO.addSuggestion(new WordSuggestionDTO(each_subjects, WordSuggestionDTO.SubjectVerbAgreementClass));
					}
				}
			}
			
			verbWordDTO.errorCode = ErrorsInBanglaLanguage.SUB_VERB_AGREEMENT_ERROR;
			

			return ErrorsInBanglaLanguage.SUB_VERB_AGREEMENT_ERROR;
		}
	}


private static List<String> getSuggestedSubject(String verb) {
int index = verbIndex.get(verb);
if (subVerbMatrix.get(index).size() > 0)
return subVerbMatrix.get(index);
return null;
}

private static boolean isSubjectVerbConflict(String subject, String verb, int namedEntityType) {
if (!verbIndex.containsKey(verb))
return false;
int index = verbIndex.get(verb);
if (namedEntityType == 0) {
if (!subVerbMatrix.get(index).contains(subject)) {
return true;
}
} else if (namedEntityType == 3) { // for organism (animals)
if (Collections.disjoint(subVerbMatrix.get(index), organismSubjects)) {
return true;
}
} else if (namedEntityType == 5) { // for person name
if (!Collections.disjoint(subVerbMatrix.get(index), uttomPurush)) {
return true;
}
}
return false;
}

	private WordDTO findSubject(SentenceDTO sentenceDTO) 
	{
		ArrayList<WordDTO> allSubject = new ArrayList<WordDTO>();
		for (WordDTO wordDTO : sentenceDTO.wordList) 
		{
			if (purushMap.containsKey(wordDTO.word)) 
			{
				allSubject.add(wordDTO);
			}
		}
		
		if (allSubject.size() == 0) 
		{
			return null;
		}
		
		if (allSubject.size() == 1) 
		{
			return allSubject.get(0);
		}
		
		if (priority1.contains(allSubject.get(0).word)) 
		{
			return allSubject.get(0);
		}
		
		if (priority1.contains(allSubject.get(1).word)) 
		{
			return allSubject.get(1);
		}
		return allSubject.get(0);
	}

	private WordDTO findVerb(SentenceDTO sentenceDTO) 
	{
		WordDTO verb = null;
		WordDTO bivokti = null;
		
		for (WordDTO wordDTO : sentenceDTO.wordList) 
		{
			String verbCandidate = removeIfNeg(wordDTO.word);
			if (isVerb(verbCandidate)) 
			{
				if (verbCandidate.substring(verbCandidate.length() - 1).equals("র")) 
				{
//					bivokti = verbCandidate;
					bivokti = wordDTO;
				} 
				else 
				{
//					verb = verbCandidate;
					verb = wordDTO;
					break;
				}
			}
		}
		
		if (verb!=null) 
		{
			return verb;
		} 
		else 
		{
			return bivokti;
		}
	}

	private static String removeIfNeg(String word) 
	{
		if (word.endsWith(NA) || word.endsWith(NI))
			return word.substring(0, word.length() - 2);
		else
			return word;
	}

public static void setPurushMap(List<GrammarDto> rows) {
purushMap = new HashMap<>();
for (GrammarDto row : rows) {
purushMap.put(row.pronoun, row.marker);
}
}

public static void buildTrie(List<GrammarDto> words) {
GrammarDto mixed;
for (int i = 0; i < words.size(); i++) {
mixed = words.get(i);
if (mixed.cholit != null && !mixed.cholit.equals("invalid")) {
verbList.insert(mixed.cholit);
}
if (mixed.shadhu != null && !mixed.shadhu.equals("invalid")) {
verbList.insert(mixed.shadhu);
}
}
}

public static void buildSubVerbMap(List<ArrayList<String>> subVerbMap) {
int index = 0;
for (int i = 0; i < subVerbMap.size(); i++) {
List<String> mixed = subVerbMap.get(i);// Arrays.asList(line.get(i).split(","));
List<String> subjects = new ArrayList<>();
if (mixed.size() > 2) {
subjects = getAllSubjects(mixed.get(2));
}

if (mixed.size() > 0 && !mixed.get(0).equals("invalid")) {
sadhuCholitMap.put(mixed.get(0), 1);
verbIndex.put(mixed.get(0), index);
subVerbMatrix.add(new ArrayList<>());
for (String subject : subjects) {
if (subVerbMatrix.get(index).contains(subject) == false)
subVerbMatrix.get(index).add(subject);
}
index++;
}
if (mixed.size() > 1 && !mixed.get(1).equals("invalid")) {
sadhuCholitMap.put(mixed.get(1), 2);
verbIndex.put(mixed.get(1), index);
subVerbMatrix.add(new ArrayList<>());
for (String subject : subjects) {
if (subVerbMatrix.get(index).contains(subject) == false)
subVerbMatrix.get(index).add(subject);
}
index++;
}
}
}

public static void addNamedEntity(TrieNodeWithList dictionary) {
namedEntity = dictionary;
}

private static boolean isNamedEntity(String word) {
return namedEntity.searchWord(word).isFound;
}

private static int getNamedEntityCategory(String word) {
Map<String, String> additional = namedEntity.searchWord(word).additional;
int category = (additional.containsKey(GrammarCheckerConstant.NAMED_ENTITY_CATEGORY)
? Integer.valueOf(additional.get(GrammarCheckerConstant.NAMED_ENTITY_CATEGORY))
: 0);
return category;
}

private static List<String> getAllSubjects(String subjects) {
if (subjects == null || subjects.length() == 0)
return new ArrayList<>();
List<String> ret = Arrays.asList(subjects.split(","));
return ret;
}

	private WordDTO findSubjectFromNamedEntity(SentenceDTO sentenceDTO) 
	{
		List<WordDTO> allSubject = new ArrayList<WordDTO>();
		for (WordDTO wordDTO : sentenceDTO.wordList) 
		{
			if (isNamedEntity(wordDTO.word)) 
			{
				int cat = getNamedEntityCategory(wordDTO.word);
				if (cat == 3 || cat == 5) 
				{
					allSubject.add(wordDTO);
				}

			}
		}
		if (allSubject.size() == 0) 
		{
			return null;
		}
		return allSubject.get(0);
	}

public static boolean isVerb(String word) {
return verbList.searchWord(word).isFound;
}

}