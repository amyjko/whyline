package edu.cmu.hcii.whyline.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Stores a dynamically adjusting array of integers that increases capacity as necessary. Like java.util.ArrayList, but for integer primitives.
 * 
 * @author Andrew J. Ko
 *
 */ 
public class IntegerVector {
	
	private int[] integers;
	private final int capacityIncrement;
	private int size = 0;
	
	public IntegerVector(int initialCapacity) {

		integers = new int[initialCapacity];
		this.capacityIncrement = initialCapacity;
		
	}

	public IntegerVector(IntegerVector vec) {
		
		integers = new int[vec.integers.length];
		size = vec.size;
		capacityIncrement = vec.capacityIncrement;
		System.arraycopy(vec.integers, 0, integers, 0, size);
		
	}
	
	public IntegerVector(DataInputStream in) throws IOException {
		
		read(in);
		capacityIncrement = size / 2;
		
	}

	public void append(int integer) {
		
		ensureCapacity();
		integers[size++] = integer;
		
	}
	
	public void push(int integer) {
		
		ensureCapacity();
		integers[size++] = integer;
		
	}
	
	public int pop() {

		if(size == 0) throw new ArrayIndexOutOfBoundsException("Stack is empty.");
		size--;
		return integers[size];
		
	}
	
	public void clear() {
		
		size = 0;
		
	}
	
	public void set(int integer, int index) {
		
		if(index < 0 || index >= size) throw new ArrayIndexOutOfBoundsException("Illegal index for IntegerVector: " + index);

		integers[index] = integer;
		
	}

	public int get(int index) { 
	
		if(index < 0 || index >= size) throw new ArrayIndexOutOfBoundsException("You bad. You sent this IntegerVector a bad index. You sent " + index + " but it was supposed to be >= 0 and < " + size);
		return integers[index]; 
		
	}
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	private void ensureCapacity() {
		
		if(size >= integers.length) {
			
			int newLength = integers.length * 2;

			int[] newData = new int[newLength];
			System.arraycopy(integers, 0, newData, 0, integers.length);
			integers = newData;
			
		}

	}

	public boolean contains(int value) { return getIndexOf(value) != -1; }

	public int getIndexOf(int value) {
			
		int low = 0;
		int high = size - 1;
		
		while(low <= high) {
			int mid = (low + high) / 2;
			int current = integers[mid];
			// After the event is no good. Bring it down.
			if(current > value)
				high = mid - 1;
			// Before the value is wrong if there are more values after this one and the next value is still before the one we're looking for.
			else if(current < value)
				low = mid + 1;
			else
				return mid;
			
		}
		
		return -1;
		
	}
	
	// Assumes that the vector is sorted. Returns -1 if there's no index before the value.
	// Doesn't necessarily return the first such index, if there are multiple indices with the same value.
	public int getIndexOfLargestValueLessThanOrEqualTo(int value) {
		
		int low = 0;
		int high = size - 1;

		while(low <= high) {
			int mid = (low + high) / 2;
			int current = integers[mid];
			// After the event is no good. Bring it down.
			if(current > value)
				high = mid - 1;
			// Before the value is wrong if there are more values after this one and the next value is still less than or equal to the given value.
			else if(current < value && mid < size - 1 && integers[mid + 1] <= value)
				low = mid + 1;
			else
				return mid;
			
		}
		
		return -1;

	}

	public int getLargestValueLessThanOrEqualTo(int value) {
		
		int index = getIndexOfLargestValueLessThanOrEqualTo(value);
		return integers[index];
		
	}
	
	public String toString() {
		
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for(int i = 0; i < size; i++) {
			if(i > 0) builder.append(" ");
			builder.append(integers[i]);
			if(i + 1 < size)
				builder.append(",");
			else
				builder.append("]");
		}
		
		return builder.toString();
		
	}

	public void trimToSize() {

		int newLength = size;

		int[] newData = new int[newLength];
		System.arraycopy(integers, 0, newData, 0, newLength);
		integers = newData;
		
	}

	public int lastValue() { return integers[size - 1]; }

	public void removeValueAt(int index) {

		// If it's the last entry, just pop.
		if(index == size - 1) pop();
		// Otherwise, copy everything after the index at the index, then subtract one from the size.
		else {
			System.arraycopy(integers, index + 1, integers, index, size - index - 1);
			size--;
		}
		
	}

	public void sortInAscendingOrder() {

		if(size > 0)
			Arrays.sort(integers, 0, size);
		
	}

	public void write(DataOutputStream out) throws IOException {

		out.writeInt(size());
		for(int i = 0; i < size; i++)
			out.writeInt(integers[i]);
		
	}

	public void read(DataInputStream in) throws IOException {

		size = in.readInt();
		integers = new int[size];
		for(int i = 0; i < size; i++)
			integers[i] = in.readInt();
		
	}
	
}