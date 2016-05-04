
package edu.cmu.hcii.whyline.ui;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.qa.UnexecutedInstruction;
import edu.cmu.hcii.whyline.source.FileInterface;

/**
 * @author Andrew J. Ko
 *
 */
public interface UserFocusListener {

	public void showInstruction(Instruction subject);
	public void showInstructions(Iterable<? extends Instruction> subject);
	public void showEvent(int eventID);
	public void showExplanation(Explanation subject);
	public void showMethod(MethodInfo subject);
	public void showFile(FileInterface subject);
	public void showUnexecutedInstruction(UnexecutedInstruction subject);
	public void showClass(Classfile subject);
	
}