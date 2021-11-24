package bangla.dao.trie;

public class TrieNode
{
	public char c;
	public boolean isWord;
	public int freq;
	public TrieNode children[];
	
	public TrieNode() 
	{
	}

	public TrieNode(char c) 
	{
		this.c = c;
		this.isWord = false;
		this.freq = 0;
	}
	
	public TrieNode addChild(char ch)
	{
		TrieNode node = new TrieNode(ch);

		if(children==null)
		{
			children = new TrieNode[1];
			children[0] = node;
		}
		else
		{		
			TrieNode tempChildren[] = new TrieNode[children.length+1];
			
			for(int i=0;i<children.length;i++)
				tempChildren[i] = children[i];
			
			int j=tempChildren.length-2;
			for(;j>=0;j--)
			{
				if(tempChildren[j].c>ch)
				{
					tempChildren[j+1]=tempChildren[j];
				}
				else break;
				
			}
			tempChildren[j+1]=node;
			children=tempChildren;
		}
		return node;
	}
	
	public TrieNode search(char ch)
	{
		if(children==null)return null;

		int i=0,j=children.length-1;
		int m;

		while(i<=j)
		{
			m=(i+j)>>1;
			
			if(children[m].c == ch )return children[m];
			
			if(children[m].c > ch)
				j=m-1;
			else
				i=m+1;
		}
		
		return null;
	}

}
