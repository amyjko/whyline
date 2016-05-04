package edu.cmu.hcii.whyline.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Stores pairs of integer ranges, such as {1-2, 5-8, 12-13}. Requires t hat numbers are added in increasing order.
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class IntegerRange {

	private final IntegerVector ranges;
	
	public IntegerRange(int initialCapacity) {

		ranges = new IntegerVector(initialCapacity);
		
	}
	
	public void include(int number) {

		if(ranges.size() > 0 && ranges.lastValue() > number)
			throw new RuntimeException(
				"Range numbers must be added in order. The most recent value was " + ranges.lastValue() + " but the next number given was " + number);
		
		// If the ranges are empty or the given number doesn't follow the upper bound of the current range, add a new range.
		if(ranges.isEmpty() || number != ranges.lastValue() + 1) {

			ranges.append(number);
			ranges.append(number);
			
		}
		// Otherwise, just update the range.
		else {
			
			ranges.set(number, ranges.size() - 1);
		
		}
		
	}
	
	public boolean contains(int number) { return getRangeIndexContaining(number) != -1; }
	
	public int getFirst() { return ranges.get(0); }
	public int getLast() { return ranges.lastValue(); }

	// Returns a range number (0-indexed) representing the range containing the given number, inclusive.
	// If no range contains this number, returns -1.
	public int getRangeIndexContaining(int number) {
		
		int index = getRangeIndexWithValueLessThanOrEqualTo(number);
		
		int lower = getLowerBoundOfRange(index);
		int upper  = getUpperBoundOfRange(index);

		// If this range contains the number, return the index. Otherwise, return -1.
		if(lower <= number && number <= upper) return index;
		else return -1;
		
	}
	
	public int getRangeIndexWithValueLessThanOrEqualTo(int number) {
		
		int index = ranges.getIndexOfLargestValueLessThanOrEqualTo(number);
		if(index < 0) return index;

		// If we got an odd index, subtract one to get to the lower bound's index.
		index -= index % 2;  
		// Get the "range index", which is just the real index halved.
		index /= 2;
		
		return index;
		
	}
	
	public int getLowerBoundOfRange(int range) { return ranges.get(range * 2); }
	public int getUpperBoundOfRange(int range) { return ranges.get(range * 2 + 1); }
	
	public boolean hasRange(int range) { return range >= 0 && range * 2 + 1 < ranges.size(); }

	public void write(DataOutputStream out) throws IOException {
		
		ranges.write(out);
		
	}

	public void read(DataInputStream in) throws IOException {
		
		ranges.read(in);
		
	}

	public void trimToSize() {
		
		ranges.trimToSize();
		
	}		

	public String toString() { 

		if(ranges.isEmpty()) return "[]";
		
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < ranges.size(); i += 2) {
			
			builder.append("[ ");
			builder.append(ranges.get(i));
			builder.append(" - ");
			builder.append(ranges.get(i + 1));
			builder.append(" ]\n");
			
		}
		return builder.toString();
		
	}
	
}
