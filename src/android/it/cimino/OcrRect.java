package it.cimino;

import org.opencv.core.Rect;

public class OcrRect extends Rect
{
	private Rect area;
	private String text;
	private int confidence;
	
	public OcrRect(Rect area, String text, int confidence){
		this.area = area;
		this.text = text;
		this.confidence = confidence;
	}

	public Rect getArea()
	{
		return area;
	}

	public void setArea(Rect area)
	{
		this.area = area;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public int getConfidence()
	{
		return confidence;
	}

	public void setConfidence(int confidence)
	{
		this.confidence = confidence;
	}
	
	
}
