package bangla;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import bangla.dto.SentenceDTO;

public class SentenceWorkerThread extends Thread 
{
	LinkedBlockingQueue<SentenceDTO> sentenceProcessQueue;
	SpellAndGrammarChecker sagc;
	boolean running;
	public int threadID;
	
	public static int THREAD_ID=1;
	static Logger logger = Logger.getLogger(SentenceWorkerThread.class);
	
	
	public SentenceWorkerThread(LinkedBlockingQueue<SentenceDTO> queue , SpellAndGrammarChecker psagc)
	{
		super(Integer.toString(THREAD_ID));
		threadID = THREAD_ID++;
		
		sentenceProcessQueue = queue;
		sagc = psagc;
		running = true;		
	}
	
	public void run()
	{
		while(running)
		{
			try
			{
				SentenceDTO sentenceDTO = sentenceProcessQueue.take();
				if(sentenceDTO.sentence==null)continue;
				logger.debug("Processing["+threadID+"]:"+sentenceDTO.sentence);
				sagc.checkSentence(sentenceDTO.parentDocumentDTO, sentenceDTO);
			}
			catch(Exception ex)
			{
				
			}
		}		
		
		logger.debug("Exiting SentenceWorkerThread:"+threadID);
	}
	
	public void shutdown()
	{
		if(running)
		{
			running=false;		
		}
	}

}
