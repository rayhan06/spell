package bangla.tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextNormalization {

	public static String normalizeText(String text) {
		return text.replaceAll("\u200A", "") // HAIR SPACE = ""
				.replaceAll("\u200B", "") // ZERO WIDTH SPACE [ZWSP]
				.replaceAll("\u200C", "") // ZERO WIDTH NON-JOINER [ZWNJ]
				.replaceAll("\u200D", "") // ZERO WIDTH JOINER [ZWJ]
				.replaceAll("\u200E", "") // LEFT-TO-RIGHT MARK = ""
				.replaceAll("\u00A0", "\u0020") // NO-BREAK SPACE [NBSP]
				.replaceAll("\u09F0", "\u09B0") // [আসামী] ৰ = র
				.replaceAll("\u09F1", "\u09AD") // [আসামী] ৱ = ভ
				.replaceAll("\u0985\u09BE", "\u0986") // অ + া = আ
				.replaceAll("\u09C7\u09BE", "\u09CB") // ে + া = ো
				.replaceAll("\u09C7\u09D7", "\u09CC") // ে + ৗ = ৌ
				.replaceAll("\u09AC\u09BC", "\u09B0") // ব + ় = র
				.replaceAll("\u09A1\u09BC", "\u09DC") // ড + ় = ড়
				.replaceAll("\u09A2\u09BC", "\u09DD") // ঢ + ় = ঢ়
				.replaceAll("\u09AF\u09BC", "\u09DF") // য + ় = য়
				.trim();
	}

	public static List<Integer> getUnicdeCodePoints(String text) {
		List<Integer> codePointContainer = new ArrayList<>();
		try {
			for (int i = 0; i < text.length(); i++) {
				codePointContainer.add(text.codePointAt(i));
			}
		} catch (IndexOutOfBoundsException indexBound) {
			System.out.println(indexBound.getMessage());
		}
		return codePointContainer;
	}

	public static Map<Integer, String> getIndexedWords(List<String> sentences) {
		Map<Integer, String> indexedWords = new HashMap<>();
		List<String> words = null;
		int baseIndex = 0;
		try {
			for (int sentence_i = 0; sentence_i < sentences.size(); sentence_i++) {
				words = WordTokenizer.tokenize(sentences.get(sentence_i));
				for (int word_i = 0; word_i < words.size(); word_i++) {
					if (words.get(word_i).length() != 0) {
						indexedWords.put(baseIndex + word_i, normalizeText(words.get(word_i)));
					}
				}
				baseIndex += words.size();
			}
		} catch (IndexOutOfBoundsException ex) {
			System.out.println(ex.getMessage());
		} catch (NullPointerException ex) {
			System.out.println(ex.getMessage());
		}
		return indexedWords;

	}
}
