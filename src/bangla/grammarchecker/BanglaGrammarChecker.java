package bangla.grammarchecker;

import bangla.dto.SentenceDTO;


public interface BanglaGrammarChecker 
{
	public int hasError(SentenceDTO sentenceDTO);
}
