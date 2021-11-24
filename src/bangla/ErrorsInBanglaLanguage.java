package bangla;

public class ErrorsInBanglaLanguage 
{

/*public static int hasError = 1;
public static int subjectVerbChecker = 2;
public static int sadhuCholitChecker = 3;
public static int cholit = 4;
public static int noneWordChecker = 5;
public static int unknownChecker = 6;
public static int nirdeshokChecker = 7;
public static int validspacemissingChecker = 8;
public static int nospaceNeededChecker = 9;
public static int complexOrInflected = 10;

public static final int ERROR_WORD=1;
public static final int WRONG_WORD_ERROR = 16;
public static final int UNKNOWN_WORD_ERROR=32;*/
//There is an error
public static final int ERROR_WORD=1;

//Subject verb agreement error
public static final int SUB_VERB_AGREEMENT_ERROR = 2;

//Shadhu cholito misron error
public static final int SHADHU_CHOLITO_MIXING_ERROR = 4;

//Punctuation error
public static final int PUNCTUATION_ERROR = 8;

//Spell checking error
public static final int WRONG_WORD_ERROR = 16;

//Unknown word error
public static final int UNKNOWN_WORD_ERROR=32;

//Nirdesok error
public static final int NIRDESHOK_ERROR = 64;

//No space between words
//All are ended with _ERROR except this due to its usage in core spellchker. Better not to change here.
public static final int NO_SPACE_PROBLEM = 128;

//Extra space between word
public static final int EXTAR_SPACE_ERROR = 256;

//যথার্থ শব্দ প্রয়োগ না করায় ভুল
public static final int IRRELEVANT_WORD_USAGE_ERROR = 512;

//অব্যয় প্রয়োগে অসঙ্গতি--1024
public static final int PREPOSITION_RELATED_ERROR = 1024;

//পুনরুক্ত বা বাহুল্যজনিত ভুল--2048
public static final int REPETITION_ERROR = 2048;

//বাক্যের গুণগত ভুল (আকাঙ্ক্ষা/আসত্তি/যোগ্যতা)--4096
public static final int QUALITY_SENTENCE_ERROR = 4096;

public static final int REAL_WORD_ERROR = 8192;
/*
বাক্যের গুণগত ভুল (আকাঙ্ক্ষা/আসত্তি/যোগ্যতা)--4096
যথার্থ শব্দ প্রয়োগ না করায় ভুল--512
প্রত্যয়-বিভক্তি/অধিযোজন প্রয়োগে অসঙ্গতি--64 --get from nirdeshok
পুনরুক্ত বা বাহুল্যজনিত ভুল--2048
সাধু ও চলিত রীতির মিশ্রণজনিত সমস্যা--4
যতি/ছেদ চিহ্নের প্রয়োগজনিত ভুল--8
এক শব্দের মাঝে ফাঁকা রাখা--256
একাধিক শব্দের মাঝে ফাঁকা না রাখা--128
পুরুষ, ক্রিয়া ও ক্রিয়ার কালজনিত ভুল--2
অব্যয় প্রয়োগে অসঙ্গতি--1024
*/

}