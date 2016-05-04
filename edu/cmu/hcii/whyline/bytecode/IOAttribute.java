package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.io.CreateGraphicsParser;
import edu.cmu.hcii.whyline.io.GetGraphicsParser;
import edu.cmu.hcii.whyline.io.GraphicalOutputParser;
import edu.cmu.hcii.whyline.io.TextualOutputParser;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.tracing.Agent;
import edu.cmu.hcii.whyline.tracing.ClassIDs;
import edu.cmu.hcii.whyline.util.IntegerVector;

/*
    SourceFile_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u4 number_of_io_instructions
    	{ u4 } classAndInstructionIDs that represent input or output
    }
 */
/**
 * @author Andrew J. Ko
 *
 */ 
public final class IOAttribute extends Attribute {

	public static final String NAME = "InputOutput";
	
	private UTF8Info attributeName;
	private ConstantPool pool;
	
	private IntegerVector ids;

	public IOAttribute(UTF8Info name, ConstantPool pool, DataInputStream data, int length) throws IOException {

		this.attributeName = name;
		this.pool = pool;
		
		int numberOfIOInstructions = data.readInt();
		this.ids = new IntegerVector(numberOfIOInstructions);
		for(int i = 0; i < numberOfIOInstructions; i++) {
			
			int instructionID = data.readInt();
			ids.append(instructionID);
			Instruction ioInstruction = pool.getClassfile().getInstructionByID(instructionID);
			ioInstruction.setIsIO();
			
		}
		
	}
	
	public IOAttribute(ConstantPool pool) throws JavaSpecificationViolation {

		this.pool = pool;
		this.attributeName = pool.addUTF8Info(NAME);

		// Empty list
		ids = new IntegerVector(100);

		Trace trace = pool.getClassfile().getTrace();
		ClassIDs classIDs = trace == null ? Agent.classIDs : trace.getClassIDs(); 
		
		// What's IO?
		int instructionID = 0;
		for(MethodInfo method : pool.getClassfile().getDeclaredMethods()) {

			CodeAttribute code = method.getCode();
			if(code != null) {

				boolean invokesTextualOutput = code.invokesTextualOutput();
				
				for(Instruction inst : code.getInstructions()) {

					boolean isIO = false;

					// Special support for instrumenting mouse events in java.awt.LightweightDispatcher.retargetMouseEvent(). The mouse event 
					// passed to this comes directly from the native window. We'll collect data about it here.
					if(inst.getIndex() == 0 && 
						inst.getMethod().getClassfile().getInternalName().equals(QualifiedClassName.get("java/awt/LightweightDispatcher")) && 
						method.getInternalName().equals("retargetMouseEvent"))

						isIO = true;

					else if(inst instanceof INVOKEVIRTUAL && GraphicalOutputParser.handles(inst))

						isIO = true;

					else if(GetGraphicsParser.handles(inst))
						
						isIO = true;

					else if(CreateGraphicsParser.handles(inst))	
						
						isIO = true;

					else if(invokesTextualOutput && TextualOutputParser.handles(classIDs, inst)) {

						isIO = true;
						
					}

					if(isIO) { 
						ids.append(instructionID);
						inst.setIsIO();
					}

					instructionID++;
										
				}

			}
			
		}
		
	}
	
	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeShort(attributeName.getIndexInConstantPool());
		bytes.writeInt(ids.size() * 4 + 4);
		bytes.writeInt(ids.size());
		for(int i = 0; i < ids.size(); i++)
			bytes.writeInt(ids.get(i));
		
	}

	public int getTotalAttributeLength() { return 2 + 4 + 4 + ids.size() * 4; }

}