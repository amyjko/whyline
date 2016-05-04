package edu.cmu.hcii.whyline.source;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class LineNumber implements Comparable<LineNumber> {

	private final FileInterface file;
	private final int number; 
	
	public LineNumber(FileInterface file, int number) {
		
		assert number > 0 : "Line numbers must be greater than 0";
		
		this.file = file;
		this.number = number;
		
	}
		
	public boolean equal(Object o) {
		
		if(!(o instanceof LineNumber)) return false;
		return ((LineNumber)o).number == number;
		
	}
	
	public boolean isAfter(LineNumber line) {
		
		assert file == line.file;
		
		return number > line.number;
		
	}
	
	public boolean isBefore(LineNumber line) {
		
		assert file == line.file;

		return number < line.number;

	}
	
	public boolean isBetweenInclusive(LineNumber before, LineNumber after) {
		
		return number >= before.number && number <= after.number;
		
	}
	
	public boolean is(LineNumber line) {
		
		assert file == line.file : "These lines are in different files: " + file + " and " + line.file;
		
		return number == line.number;
		
	}
	
	public int getNumber() { return number; }
	public FileInterface getFile() { return file; }
	
	public String toString() { return "" + file.getShortFileName() + ":" + number; }

	public int compareTo(LineNumber o) {
		
		if(file == o.file)
			return number - o.number;
		else
			return file.compareTo(o.file);
		
	}
	
}
