package bangla.dao.trie;

import java.util.*;
import org.apache.log4j.Logger;
import bangla.dto.*;

public class TrieRepository 
{	
	static Logger logger = Logger.getLogger(TrieRepository.class);
	private static TrieRepository trieRepository = null;
	
	public static  int MAX_EDIT_DISTANCE=2;
	public static final int MIN_FREQUENCY=1;
	
	public static int nodeCount=0;
	TrieNode root;
	
	private class Encaptulator
	{
		public int currentIndex;
		public String currentWord;
		int misMatch;
		TrieNode node;
		
		public Encaptulator(int curr, String currWord, int misMatch, TrieNode node) 
		{
			this.currentIndex = curr;
			this.currentWord = currWord;
			this.misMatch = misMatch;
			this.node = node;
		}
	}
	
	private TrieRepository()
	{
		root = new TrieNode();
	}
	
	public static TrieRepository getInstance()
	{
		if(trieRepository==null)
		{
			createTrieRepository();
		}
		return trieRepository;
	}
	
	private static synchronized void createTrieRepository()
	{
		if(trieRepository==null)
			trieRepository = new TrieRepository();
	}
	
	public  void insert(String word) 
	{
		TrieNode recurseRoot = root;
		
		for(int i=0; i < word.length(); i++) 
		{
			char c = word.charAt(i);
			TrieNode temp=recurseRoot.search(c);

			if(temp==null) 
			{
				temp = recurseRoot.addChild(c);
				nodeCount++;
			}
	
			if(i==word.length() - 1)
			{
				temp.isWord = true;
				break;
			}
			recurseRoot = temp;
		}
		return;
	}
	
	public  boolean searchWord(String word) 
	{
		TrieNode start = root;

		int wordLen=word.length();
		for(int i=0;i<wordLen;i++) 
		{
			char c = word.charAt(i);
			TrieNode temp = start.search(c);
			if(temp==null)return false;

			if(i==wordLen-1) 
			{
				if(temp.isWord) 
					return true;
			}
			start=temp;
		}
		return false;
	}

	public  int searchWordPrefix(String word) 
	{
		TrieNode start = root;
		int prefixLen=-1;
		int wordLen=word.length();
		for(int i=0;i<wordLen;i++) 
 		{
			char c = word.charAt(i);
			TrieNode temp = start.search(c);
			if(temp==null)break;
			
			if(temp.isWord)	prefixLen = i+1;
			start=temp;
		}
		return prefixLen;
	}

	public void getSuggestedWord(WordDTO pWordDTO, int maxEditDistance) 
	{
		if(maxEditDistance<=0)maxEditDistance=MAX_EDIT_DISTANCE;

		long startTime = System.nanoTime();
		
		int currentSuggestionCount = 0;
		if(pWordDTO.wordSuggestionList!=null) currentSuggestionCount = pWordDTO.wordSuggestionList.size();
		
		char wordFirstChar = pWordDTO.word.charAt(0);
		for(TrieNode node: root.children) 
		{
			
			if (isConfusableChar(node.c, wordFirstChar)) 
			{
				dictionaryTraverse(pWordDTO,pWordDTO.word, node, maxEditDistance);
			}
		}
		
		long endTime = System.nanoTime();

// Printling log record only		
		StringBuffer resultString=null;
		if(pWordDTO.wordSuggestionList!=null)
		{
			for( WordSuggestionDTO wordSuggestionDTO: pWordDTO.wordSuggestionList)
			{				
				if(currentSuggestionCount<=0)
				{
					if(resultString==null)
					{
						resultString = new StringBuffer("Suggestion Words:");
						resultString.append(wordSuggestionDTO.suggestionWord);
					}
					else
					{
						resultString.append(", ");
						resultString.append(wordSuggestionDTO.suggestionWord);
					}
					resultString.append(':');
					resultString.append(wordSuggestionDTO.frequency);

				}
				currentSuggestionCount--;
			}
		}
		logger.debug("Running Edit Distance based suggestion generation. Time taken:"+(endTime - startTime)+" nano seconds");
		logger.debug("Input Word:"+pWordDTO.word);
		logger.debug(resultString);
// End Of: Printling log record only		
	}

	public boolean isConfusableChar(char a, char b)
	{
		if(a==b)
			return true;
		if(a>b)
		{
			char c = a;
			a=b;
			b=c;
		}
		//Now a is small and b is big
		
		switch(a)
		{
		case 'অ':	if(b=='আ'||b=='য়')return true;
		break;
		case 'ই':	if(b=='ঈ')return true;
		break;
		case 'উ':	if(b=='ঊ')return true;
		break;
		case 'ঋ':	if(b=='র')return true;
		break;
		case 'ক':	if(b=='খ')return true;
		break;
		case 'গ':	if(b=='ঘ')return true;
		break;
		case 'ঙ':	if(b=='ং')return true;
		break;
		case 'চ':	if(b=='ছ')return true;
		break;
		case 'জ':	if(b=='য'||b=='ঝ')return true;
		break;
		case 'ট':	if(b=='ঠ')return true;
		break;
		case 'ড':	if(b=='ঢ')return true;
		break;
		case 'ন':	if(b=='ণ')return true;
		break;
		case 'শ':	if(b=='ষ'||b=='স')return true;
		break;
		case 'র':	if(b=='ড়'||b=='ঢ়')return true;
		break;
		case 'ত':	if(b=='ৎ'||b=='থ')return true;
		break;
		case 'থ':	if(b=='ঠ')return true;
		break;
		case 'প':	if(b=='ফ')return true;
		break;
		case 'ব':	if(b=='ভ')return true;
		break;
		}
		return false;
	}
	
	public void dictionaryTraverse(WordDTO pWordDTO, String word,  TrieNode node, int maxEditDistance) 
	{
		String source = "";
		Encaptulator  bfsNode= new Encaptulator(1,source , 0, node);		
		
		Queue<Encaptulator> Q = new LinkedList<Encaptulator>(); 		
		Q.add(bfsNode);

//		HashMap<String,WordSuggestionDTO> minCostMap = new HashMap<String,WordSuggestionDTO>();
		
		while (Q.size() > 0) 
		{
			bfsNode = (Encaptulator)Q.remove();
			if(bfsNode.misMatch> maxEditDistance) 
			{
				logger.debug("Removed one Node");
				continue;
			}
			if(bfsNode.node.isWord  && Math.abs(bfsNode.currentIndex  - word.length()) < 2 )
			{
				String suggestionWord = bfsNode.currentWord + bfsNode.node.c ;
				int suggestionCost = bfsNode.misMatch;
				if(bfsNode.currentIndex  - word.length() < 0)
					suggestionCost +=  word.length() - bfsNode.currentIndex ;
				
	//			WordSuggestionDTO oldSuggestionPair = minCostMap.get(suggestionWord);
//				if(oldSuggestionPair == null )
				{
					if(suggestionCost <= maxEditDistance) 
					{
						WordSuggestionDTO suggestionPair =new WordSuggestionDTO(suggestionWord, WordSuggestionDTO.EditDistanceBasedSuggestionClass, suggestionCost); 
						pWordDTO.addSuggestion(suggestionPair);
//						minCostMap.put(suggestionWord, suggestionPair);
					}
				}
//				else
				{
	//				if(oldSuggestionPair.editDistance > suggestionCost)
					{
		//				oldSuggestionPair.editDistance = suggestionCost; 
					}
				}
			}
									
			int costAddition = 1;	
			String tempCurrWord = bfsNode.currentWord + bfsNode.node.c;
			if(bfsNode.node.children!=null)
			for(TrieNode childNode : bfsNode.node.children) 
			{
				// avoid the Array index out of range 		
				if(bfsNode.currentIndex < word.length() && word.charAt(bfsNode.currentIndex) == childNode.c)
				{
					costAddition = 0;
				}
				else 
					costAddition = 1;
				if(bfsNode.misMatch+ costAddition <= maxEditDistance)
					Q.add(new Encaptulator(bfsNode.currentIndex + 1, tempCurrWord, bfsNode.misMatch + costAddition, childNode));
				if(bfsNode.misMatch+ 1 <= maxEditDistance)
					Q.add(new Encaptulator(bfsNode.currentIndex , tempCurrWord, bfsNode.misMatch + 1, childNode));
			}
			//this statement serving the purpose of deletion
			if(bfsNode.misMatch+ 1 <= maxEditDistance)
				Q.add(new Encaptulator(bfsNode.currentIndex + 1 , bfsNode.currentWord, bfsNode.misMatch + 1, bfsNode.node));
			} 
		Q.clear();
	}
	
	public static void main(String args[])
	{
		String dictionary[]= {"book","bat","batman","apple","boom", "charm", "call", "proper", "proprietery", "crawler"
				,"dire", "consequences", "devastating", "repercussion",
				"knock", "down", "destroy", "something", 
				"walk", "into", "someones", "shoes", "be", "in","position",
				"crossing", "my", "mind", "thinking", "over", "something",
				"fingers", "crossing", "along",
				"fail", "keep", "promise", "change", "decision", "agreement",
				"arrive", "reach",
				"involve", "deep",
				"mouth",
				"get", "into", "everyones", "hair", "turn", "someone" ,"over", "acquisitive", "too","keenness", "calling", "strong", "urge",
				"knock",
				"disband",
				"scatter",
				"sick", "my", "stomach", "extremely", "sorry",
				"rome", "was", "not", "built", "day", "takes", "significant",
				"impractible", "unachievable", "infeasible", "unworkable",
				"elusive", "very", "hard", "achieve", "acquire",
				"frame", "of", "mind", 
				"point", "stand",
				"pull", "me", "through", "whole", "problem", "time", "hand", "someones", "times", "occasionally", 
				"rambling", "wandering", "aimelessly"
				,"distraught", "extremely", "worried", "sad",
				"take", "me", "remembering", "old", "times", "and","events",
				"ruminate", "chew", "abnegate", "not", "allow", "yourself", "have", "something",
				"strip", "of", "clothes", "swathe", "mystifying", "dazed", "confused", "opaque", "all", "over", "the", "place", "muzzy", "dizzying",
				"free", "flowing", "care", "giving", "going", "carefree",
				"easy", "come", "go", 
				"your", "leisure", "at", "your", "time",
				"is", "the", "need",  "hour", "necessary", "current", "situation",
				"come", "out", "appear", "in", "my", "skin",
				"set", "someone", "up", "help", "something", "establish",
				"that", "will", "create", "career", "path", "for", "his", "life",
				"deep", "pockets", "has", "lot", "of", "money",
				"afflict"};
		String dictionaryBangla[] = {"পেশাদার","কৃষক","কিন্ডারগার্টেন","রক্ষাণাবেক্ষণ"};
		
		TrieRepository trie = TrieRepository.getInstance();
		for(int i=0; i < dictionary.length; i++)
		{
			trie.insert(dictionary[i]);
		}
		
		
		String searchWord[]    = {"book", "cat", "water", "boom", "ball", "charm", "call", "aple"};
		boolean searchDecision[]= { true , false,  false , true  ,  false,  true  , true  , false};
		
		
		for(int i=0;i<searchWord.length;i++)
		{
			boolean result = trie.searchWord(searchWord[i]);
			System.out.println("SearchWord:" + searchWord[i] + " = " + result+ " Orig:"+searchDecision[i]);
			System.out.println();
		}
		
		String prefixSearchWord[]= {"comeback", "out", "hardship", "disband", "ball", "charm", "call", "aple"};
		
		for(int i=0;i<prefixSearchWord.length;i++)
		{
			System.out.println("Prefix SearchWord:"+prefixSearchWord[i]+" prefixlength:"+trie.searchWordPrefix(prefixSearchWord[i]));
		}
		
		String wrongWord[]={"bok", "crawmer", "book", "appl", "carm", "property","proprieter","aple"};
		String wrongWordBangla[] = {"proprieter"};// {"রক্ষণাবেক্ষন","কিণ্ডারগার্টেন"};
		for(int i=0; i < wrongWord.length; i++)
		{
			/*ArrayList<WordSuggestionDTO> list = trie.getSuggestedWord(wrongWord[i]);
			if(list==null)
				System.out.println("No Suggestion found for :" + wrongWord[i]);
			else
			{
				System.out.println("Suggestions for :" + wrongWord[i]);
				for(WordSuggestionDTO pair: list)
				{
					System.out.print(pair.suggestionWord+ "(" + pair.score + "),");
				}
				System.out.println();
			}*/
		}
		
		
	}
	
	}
