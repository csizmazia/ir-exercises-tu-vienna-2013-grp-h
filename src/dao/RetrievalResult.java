package dao;


public class RetrievalResult implements Comparable<RetrievalResult>
{
	public String size;
	private String topic;
	private final static String group = "groupH";
	private float similarity;
	private int placement;
	private String documentClass;
	private String documentName;
	
	private int topicNumber;
	
	public void setSimilarity(float similarity)
	{
		this.similarity = similarity;
	}
	
	public String getDesiredFilename()
	{
		//medium_topic1_group10.txt
		String retval = size+"_"+"topic"+topicNumber + "_"+group+".txt";
		return retval;
	}


	public float getSimilarity()
	{
		return similarity;
	}
	
	public void setTopicNumber (int topicNum)
	{
		this.topicNumber = topicNum;
	}


	public String getDocumentClass()
	{
		return documentClass;
	}


	public void setSize(String size)
	{
		this.size = size;
	}


	public void setTopic(String topic)
	{
		this.topic = topic;
	}


	public void setPlacement(int placement)
	{
		this.placement = placement;
	}


	public void setDocumentClass(String documentClass)
	{
		this.documentClass = documentClass;
	}


	public void setDocumentName(String documentName)
	{
		this.documentName = documentName;
	}


	public String getDocumentName()
	{
		return documentName;
	}


	public RetrievalResult(String _topic, String _size, float _distance, int _placement, String _documentClass, String _documentName)
	{
		topic = _topic;
		size = _size;
		similarity = _distance;
		placement = _placement;
		documentClass = _documentClass;
		documentName = _documentName;
	}
	
	
	public RetrievalResult()
	{
	}
	
	@Override
	public boolean equals(Object another)
	{
		/*
		if (another.getClass() == this.getClass())
		{
			
		}
		*/
		System.out.println();
		return false;
	}


	@Override
	public String toString()
	{
		return "topic"+topicNumber + " Q0 "+documentClass +"/"+ documentName+ " "+placement + " "+similarity + " "+group+"_" + size;
	}


	@Override
	public int compareTo(RetrievalResult o)
	{
		return Float.compare(this.similarity, o.similarity);
	}
}
