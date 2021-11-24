package bangla.tokenizer;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import bangla.dto.DocumentDTO;
import bangla.dto.SentenceDTO;


public class SentenceTokenizer {


	static Logger logger = Logger.getLogger(SentenceTokenizer.class);

	private static final char char_0021 = '\u0021'; // U+0021 : EXCLAMATION MARK -> !
//	private static final char char_002E = '\u002E'; // U+002E : FULL STOP {dot} -> .
	private static final char char_003B = '\u003B'; // U+003B : SEMICOLON -> ;
	private static final char char_003F = '\u003F'; // U+003F : QUESTION MARK -> ?
	private static final char char_007C = '\u007C'; // U+007C : VERTICAL LINE {vertical bar} -> |
	private static final char char_0964 = '\u0964'; // U+0964 : DEVANAGARI DANDA {purna viram} -> ।
	private static final char char_0965 = '\u0965'; // U+0965 : DEVANAGARI DOUBLE DANDA {deergh viram} -> ॥
	private static final char char_09F7 = '\u09F7'; // U+09F7 : BENGALI CURRENCY NUMERATOR FOUR -> ৷

	/*
	 * উদ্ধৃতি :
	 * \u201c - left double quotation mark
	 * \u201d - right double quotation mark
	 * \u2018 - left single quotation mark
	 * \u2019 - right single quotation mark
	 * \u0027 - single quotation or apostrophe
	 * \u0022 - double quotation or quotation mark
	 */
	private static final String regex_bn_double_quote = "[^!?।॥৷\\|]+[,:–—]?\\s*\\u201c(.+?)\\s*([!?।॥৷\\|]+\\u201d|\\u201d\\s*[!?।॥৷\\|]+)";
	private static final String regex_bn_single_quote = "[^!?।॥৷\\|]+[,:–—]?\\s*\\u2018(.+?)\\s*([!?।॥৷\\|]+\\u2019|\\u2019\\s*[!?।॥৷\\|]+)";
	private static final String regex_en_double_quote = "[^!?।॥৷\\|]+[,:–—]?\\s*\\u0022(.+?)\\s*([!?।॥৷\\|]+\\u0022|\\u0022\\s*[!?।॥৷\\|]+)";
	private static final String regex_en_single_quote = "[^!?।॥৷\\|]+[,:–—]?\\s*\\u0027(.+?)\\s*([!?।॥৷\\|]+\\u0027|\\u0027\\s*[!?।॥৷\\|]+)";

	/*
	 * \u0028 - left parenthesis '('
	 * \u0029 - right parenthesis ')'
	 *
	 * \u007b - left curly bracket '{'
	 * \u007d - right curly bracket '}'
	 *
	 * \u005b - left square bracket '['
	 * \u005d - right square bracket ']'
	 */
	private static final String regex_parenthesis = "[^!?।॥৷\\|]*\\u0028([^\\u0028\\u0029]*)\\u0029[^!?।॥৷\\|]*[!?।॥৷\\|]";
	private static final String regex_curly_bracket = "[^!?।॥৷\\|]*\\u007b([^\\u007b\\u007d]*)\\u007d[^!?।॥৷\\|]*[!?।॥৷\\|]";
	private static final String regex_square_bracket = "[^!?।॥৷\\|]*\\u005b([^\\u005b\\u005d]*)\\u005d[^!?।॥৷\\|]*[!?।॥৷\\|]";

	public static void sentenceTokenizer(DocumentDTO pDocumentDTO) {
		int startIndex = 0;
		for (int i = 0; i < pDocumentDTO.contentChar.length; i++) {
			int trailingSentenceEndChar = isSentenceSeparator(pDocumentDTO.contentChar[i]);
			if (trailingSentenceEndChar >= 0 || i == pDocumentDTO.contentChar.length - 1) {
				if (startIndex == i) startIndex++;
				else {
					int sentenceLength = i - startIndex;
					if (trailingSentenceEndChar > 0)
						sentenceLength += trailingSentenceEndChar;
					else
						sentenceLength += 1;

					if (hasAnyBanglaWord(pDocumentDTO.contentChar, startIndex, sentenceLength)) {
						String sentenceStr = new String(pDocumentDTO.contentChar, startIndex, sentenceLength);
						SentenceDTO sentenceDTO = new SentenceDTO(sentenceStr, startIndex, sentenceLength);
						pDocumentDTO.addSentence(sentenceDTO);
						logger.debug("Sentence:" + sentenceDTO.sentence + " , Offset:" + sentenceDTO.offset + " , Length:" + sentenceDTO.length + " , StringLength:" + sentenceDTO.length);
					}
					startIndex = i + 1;
				}
			}
		}
	}

	public static ArrayList<String> sentenceTokenizer(String content) {
		ArrayList<String> sentenceList = new ArrayList<String>();
		char contentChar[] = content.toCharArray();
		int startIndex = 0;
		for (int i = 0; i < contentChar.length; i++) {
			int trailingSentenceEndChar = isSentenceSeparator(contentChar[i]);
			if (trailingSentenceEndChar >= 0 || i == contentChar.length - 1) {
				if (startIndex == i) startIndex++;
				else {

					if (hasAnyBanglaWord(contentChar, startIndex, i - startIndex + trailingSentenceEndChar)) {
						String sentenceStr = new String(contentChar, startIndex, i - startIndex + trailingSentenceEndChar);
						sentenceList.add(sentenceStr);
					}
					startIndex = i + 1;
				}
			}
		}

		return sentenceList;
	}

	private static int isSentenceSeparator(char ch) {
		switch (ch) {
			case '\r':
				return 0;

			case '\n':
				return 0;

			case '।':
				return 1;
			case '?':
				return 1;
			case '!':
				return 1;
			default:
				return -1;

		}

	}

	private static boolean hasAnyBanglaWord(char content[], int offset, int length) {
		for (int i = offset; i < offset + length; i++) {
			if ((content[i] >= 0x0980 && content[i] <= 0x09E3) || content[i] == '-')
				return true;
		}
		return false;
	}


}
