package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class ConstantPoolInfo {
	
	protected final ConstantPool pool;
	private final int index;
	
	public ConstantPoolInfo(ConstantPool pool) {
		
		this.pool = pool;
		this.index = pool.getSize();
		
	}

	public abstract void resolveDependencies();
	
	public abstract void toBytes(DataOutputStream bytes) throws IOException;
	
	public ConstantPool getPool() { return pool; }
	
	public int getIndexInConstantPool() { return index; }

	public Classfile getClassfile() { return pool.getClassfile(); }
	
}
