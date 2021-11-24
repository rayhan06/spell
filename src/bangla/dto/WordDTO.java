package bangla.dto;

import java.util.ArrayList;

public class WordDTO 
{
	public String word;
	public int offset, length;
	
	public int errorCode;	
	public ArrayList<WordSuggestionDTO> wordSuggestionList;
	
	public SentenceDTO parentSentenceDTO;	
	
	public WordDTO prev,next;
	public WordDTO()
	{
		offset=-1;
		length=0;
		word=null;
		errorCode=0;
		wordSuggestionList = null;
		prev=next=null;
	}
	
	public WordDTO(char content[], int pOffset, int pLength, char output[])
	{
		offset = pOffset;
		length = pLength;
		errorCode= 0;
		wordSuggestionList = null;
		prev=next=null;
		
		int outputi=0;
		
		for(int i=pOffset;i<pOffset+pLength;i++)
		{
			if(content[i]>=0x200A&&content[i]<=0x200E)
				continue;
			
			
			switch(content[i])
			{
			case 0x00A0:
				output[outputi++]=0x0020;
				break;
			case 0x09F0:
				output[outputi++]=0x09B0;
				break;
			case 0x09F1:
				output[outputi++]=0x09AD;
				break;
				
			case 0x0985: 
				if(i+1<pOffset+pLength && content[i+1]==0x09BE)
				{
					output[outputi++]=0x0986;
					i++;
				}
				else
					output[outputi++]=0x0985;
				break;
				
			case 0x09C7:
				if(i+1<pOffset+pLength)
				{
					if(content[i+1]==0x09BE)
					{
						output[outputi++]=0x09CB;
						i++;
					}
					else if(content[i+1]==0x09D7)
					{
						output[outputi++]=0x09CC;
						i++;
					}
					else output[outputi++]=0x09C7;
				}
				else
					output[outputi++]=0x09C7;
				break;
			
			
			case  0x09AC:
				if(i+1<pOffset+pLength&&content[i+1]==0x09BC)
				{
					output[outputi++]=0x09B0;
					i++;
				}
				else output[outputi++]=0x09AC;
			break;
			
			case 0x09A1:
				if(i+1<pOffset+pLength&&content[i+1]==0x09BC)
				{
					output[outputi++]=0x09DC;
					i++;
				}
				else output[outputi++]=0x09A1;
			break;
			
			case 0x09A2:
				if(i+1<pOffset+pLength&&content[i+1]==0x09BC)
				{
					output[outputi++]=0x09DD;
					i++;
				}
				else output[outputi++]=0x09A2;
			break;
			
			case 0x09AF:
				if(i+1<pOffset+pLength&&content[i+1]==0x09BC)
				{
					output[outputi++]=0x09DF;
					i++;
				}
				else output[outputi++]=0x09AF;					
			break;
			default:
				output[outputi++]=content[i];
				break;
			
			}
		}
		
		word = new String(output,0,outputi);
	}
	
	/*
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
	}*/
	
	
	public void addSuggestion(WordSuggestionDTO pWordSuggestionDTO)
	{
		if(wordSuggestionList==null)
			wordSuggestionList = new ArrayList<WordSuggestionDTO>();
		else
		{
			for(WordSuggestionDTO existingDTO:wordSuggestionList)
			{
				if(existingDTO.suggestionWord.equals(pWordSuggestionDTO.suggestionWord))
				{
					if(existingDTO.suggestionClass>pWordSuggestionDTO.suggestionClass)
					{
						existingDTO.suggestionClass=pWordSuggestionDTO.suggestionClass;
						existingDTO.frequency = pWordSuggestionDTO.frequency;
					}
					if(existingDTO.suggestionClass==pWordSuggestionDTO.suggestionClass)
					{
						if(existingDTO.frequency < pWordSuggestionDTO.frequency)						
							existingDTO.frequency = pWordSuggestionDTO.frequency;
						if(existingDTO.editDistance<pWordSuggestionDTO.editDistance)
							existingDTO.editDistance=pWordSuggestionDTO.editDistance;
					}

					return;
				}
			}
		}
		
		wordSuggestionList.add(pWordSuggestionDTO);
	}
	
	public void sortSuggestion()
	{
		if(wordSuggestionList==null)return;
		for(WordSuggestionDTO wS:wordSuggestionList)wS.calculateStatisticalProbability();
		
		int size = wordSuggestionList.size();
		for(int i=0;i<size-1;i++)
		{
			WordSuggestionDTO wsi= wordSuggestionList.get(i);
			if(wsi.suggestionClass==WordSuggestionDTO.NaturalErrorSuggestionClass)continue;
			for(int j=i+1;j<size;j++)
			{
				
				WordSuggestionDTO wsj= wordSuggestionList.get(j);	
				
				if(wsj.suggestionClass==WordSuggestionDTO.NaturalErrorSuggestionClass)
				{
					wordSuggestionList.set(i, wsj);
					wordSuggestionList.set(j, wsi);
					wsi=wsj;
				}
				else if(wsi.statisticalProbability< wsj.statisticalProbability)
				{
					wordSuggestionList.set(i, wsj);
					wordSuggestionList.set(j, wsi);
					wsi=wsj;					
				}
				
				else if(wsi.statisticalProbability== wsj.statisticalProbability&&wsi.suggestionClass>wsj.suggestionClass)
				{
					wordSuggestionList.set(i, wsj);
					wordSuggestionList.set(j, wsi);
					wsi=wsj;
				}
				else if(wsi.statisticalProbability== wsj.statisticalProbability&&wsi.suggestionClass==wsj.suggestionClass&&wsi.suggestionClass==WordSuggestionDTO.EditDistanceBasedSuggestionClass)
				{
					if((wsi.editDistance>wsj.editDistance)||
							((wsi.editDistance==wsj.editDistance)&&(wsi.frequency<wsj.frequency)))
					{
						wordSuggestionList.set(i, wsj);
						wordSuggestionList.set(j, wsi);
						wsi=wsj;						
					}
				}				
				else if(wsi.statisticalProbability== wsj.statisticalProbability&&wsi.suggestionClass==wsj.suggestionClass)
				{
					if(wsi.frequency<wsj.frequency)
					{
						wordSuggestionList.set(i, wsj);
						wordSuggestionList.set(j, wsi);
						wsi=wsj;						
					}
				}
				
			}
		}
	}
}
