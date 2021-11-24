package bangla.tokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WordTokenizer_OLD {

	private static final String bn_char_alpha = "অআইঈউঊঋঌএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহড়ঢ়য়ৠৡঁংঃ়ৎািীুূৃৄেৈোৌ্ৗৢৣ";
	private static final String bn_char_numeric = "০১২৩৪৫৬৭৮৯";

	private static final String bn_char_alphanumeric = bn_char_alpha + bn_char_numeric;

	private static final String regex_alpha = "[" + bn_char_alpha + "]+";
	private static final String regex_numeric = "[" + bn_char_numeric + "]+";

	private static List<String> spliting(String text) {
		return Arrays.asList(text.split("[ \\/\\r\\n\\t+*]+"));
	}

	private static String trim_left(String text) {
		return text.replaceAll("^[^\\s*" + bn_char_alphanumeric + "\\s*]+", "");
	}

	private static String trim_right(String text) {
		return text.replaceAll("[^\\s*" + bn_char_alphanumeric + "\\s*]+$", "");
	}

	private static String replace_punctuation(String text) {
		return text.replaceAll("[!\\?\\|।॥৷…\"“”‘’\\,;–—(\\[{)}\\]]+", " ");
	}

	private static String replace_hyphen(String text) {
		return text.replaceAll("[-]+", "-");
	}

	private static String replace_star(String text) {
		return text.replaceAll("[\\*]+", "");
	}

	private static String replace_hash(String text) {
		return text.replaceAll("[#]+", "");
	}

	private static String replace_at(String text) {
		return text.replaceAll("[@]+", "");
	}

	private static String replace_group(String text) {
		return text.replaceAll("(\\r\\n)|\\s|(<VBCRLF>|\\n)", " ");
	}

	private static List<String> split_compound(String text) {
		String[] str = text.split("-");
		return Arrays.asList(str);
	}

	private static String valid_numeric(String text) {
		if (getAllMatches(text, regex_numeric).size() == 0)
			return text;
		return null;
	}

	private static String valid_alpha(String text) {
		if (getAllMatches(text, regex_numeric).size() > 0) {
			List<String> res = getAllMatches(text, "^" + regex_numeric + regex_alpha);
			if (res.size() > 0) {
				text = res.get(0);
			}
		}
		return text;
	}

	public static List<String> tokenize(String text) {
		if (text == null)
			throw new NullPointerException("text field can not be null");
		
		text = replace_group(text);
		text = replace_punctuation(text);
		List<String> words = spliting(text);
		List<String> result = new ArrayList<String>();
		for (String word : words) {

			word = replace_hyphen(word);
			word = replace_star(word);
			word = replace_hash(word);
			word = replace_at(word);

			List<String> tWord = Arrays.asList(word).stream()
			// List<String> tWord = split_compound(word).stream()
					.map(t -> valid_numeric(t)).filter(t -> t != null)
					.map(t -> valid_alpha(t)).filter(t -> t != null)
					.map(t -> trim_right(t)).filter(t -> t != null)
					.map(t -> trim_left(t)).filter(t -> t != null)
					.map(t -> t.trim())
					.filter(t -> t.length() > 0)
					.collect(Collectors.toList());

			// System.out.println("Word length can be of any size except zero");
			result.addAll(tWord);
		}
		return result;
	}

	private static List<String> getAllMatches(String text, String regex) {
		List<String> matches = new ArrayList<String>();
		Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
		while (m.find()) {
			matches.add(m.group(1));
		}
		return matches;
	}

}
