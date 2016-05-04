package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.util.Named;

public class Name implements Named {

	private final String name;
	
	public Name(String name) {
		
		this.name = name;		
		
	}
	
	public String getDisplayName(boolean html, int lengthLimit) { return name; }
	
}
