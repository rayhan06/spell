package bangla.withtrie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bangla.grammarchecker.GrammarCheckerConstant;

public class TrieNodeWithList {
	public char c;
	public boolean isWord;
	public int reference_table;
	public long reference_id;
	public String viceVersaWord;
	public TrieNodeWithList parent;
	public int namedEntityCategory = 0;
	public int freq;
	/*following are the new attributes require for aho-coarisck algorithm*/
	public HashMap<String, Integer> child = new HashMap<String, Integer>();            
	public long wordID = -1;
	public char parentChar;
	/*aho coarisck parameter list ended here*/
	
	public List<TrieNodeWithList> children = new ArrayList<>();

	public TrieNodeWithList() {
		parent = null;
	}

	public TrieNodeWithList(char c) {
		this.c = c;
		parent = null;
		this.isWord = false;
		this.reference_id = -1;
		this.reference_table = -1;
		this.freq = 0;
	}


	public char getChar() {
		return this.c;
	}

	public long getReferenceId() {
		return this.reference_id;
	}

	public int getReferenceTable() {
		return this.reference_table;
	}

	public TrieNodeWithList getParent() {
		return parent;
	}

	public List<TrieNodeWithList> getChildren() {
		return children;
	}


	public TrieNodeSearchResult searchWord(String word) {
		return this.searchWord(word, false);
	}

	public TrieNodeSearchResult searchWord(String word, boolean isAllowPartialMatch) {
		List<TrieNodeWithList> child = this.children;
		int lastMatch = -1;
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			TrieNodeWithList temp = null;
			boolean isFound = false;
			for (TrieNodeWithList node : child) {
				if (node.c == c) {
					temp = node;
					isFound = true;
					break;
				}
			}
			if (temp != null && temp.isWord && isAllowPartialMatch) {
				lastMatch = i;
			}
			if (!isFound) {
				Map<String,String> additional = new HashMap<>();
				additional.put(GrammarCheckerConstant.NAMED_ENTITY_CATEGORY, "0");
				return new TrieNodeSearchResult((lastMatch == -1) ? false : true, null, lastMatch, additional);
			}
			child = temp.children;
			if (i == word.length() - 1) {
				if (temp.isWord) {
					Map<String,String> additional = new HashMap<>();
					additional.put(GrammarCheckerConstant.NAMED_ENTITY_CATEGORY, String.valueOf(temp.namedEntityCategory));
					return new TrieNodeSearchResult(true, temp.viceVersaWord, word.length() - 1, additional);
				}
			}
		}
		Map<String,String> additional = new HashMap<>();
		additional.put(GrammarCheckerConstant.NAMED_ENTITY_CATEGORY, "0");
		return new TrieNodeSearchResult((lastMatch == -1) ? false : true, null, lastMatch, additional);
	}

	public void insert(String word) {
		this.insert(word, null);
	}

	public void insert(String word, String viceVersa) {
		List<TrieNodeWithList> child = this.children;
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			TrieNodeWithList temp = null;
			boolean isFound = false;
			for (TrieNodeWithList node : child) {
				if (node.c == c) {
					temp = node;
					isFound = true;
					break;
				}
			}
			if (!isFound) {
				temp = new TrieNodeWithList(c);
				child.add(temp);
			}
			child = temp.children;
			if (i == word.length() - 1) {
				temp.viceVersaWord = viceVersa;
				temp.isWord = true;
			}
		}
	}
}
