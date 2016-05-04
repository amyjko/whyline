package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ConstantPoolPadding extends ConstantPoolInfo {

	public ConstantPoolPadding(ConstantPool pool) { super(pool); }

	public void toBytes(DataOutputStream bytes) throws IOException {}

	public void resolveDependencies() {}

}
