package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class BytecodeParser {

	public static Instruction[] parse(DataInputStream reader, int numberOfBytesInCodeArray, CodeAttribute owner) throws IOException {
		
		ConstantPool pool = owner == null ? null : owner.getMethod().getClassfile().getConstantPool();
		
		Instruction[] instructions = new Instruction[numberOfBytesInCodeArray];
		
		int index = 0;
		
		int bytesRemaining = numberOfBytesInCodeArray;

		int remainder = 0;
    	int padding = 0;

    	// Construct a structured version of the bytecode
		while(bytesRemaining > 0) {

			int opcode = reader.readUnsignedByte();

			Instruction instruction = null;

			int byteIndex = numberOfBytesInCodeArray - bytesRemaining;
			int byteLength = 0;
			
			switch(opcode) {
			
				case Opcodes.AALOAD: 			instruction = new AALOAD(owner); break;
				case Opcodes.AASTORE: 			instruction = new AASTORE(owner); break;
				case Opcodes.ACONST_NULL: 	instruction = new ACONST_NULL(owner); break;
				case Opcodes.ALOAD: 				instruction = new ALOAD(owner, reader.readUnsignedByte()); break;
				case Opcodes.ALOAD_0: 			instruction = new ALOAD_0(owner); break;
				case Opcodes.ALOAD_1: 			instruction = new ALOAD_1(owner); break;
				case Opcodes.ALOAD_2: 			instruction = new ALOAD_2(owner); break;
				case Opcodes.ALOAD_3: 			instruction = new ALOAD_3(owner); break;
				case Opcodes.ANEWARRAY: 		instruction = new ANEWARRAY(owner, (ClassInfo)pool.get(reader.readUnsignedShort())); break;
				case Opcodes.ARETURN: 			instruction = new ARETURN(owner); break;
				case Opcodes.ARRAYLENGTH: 	instruction = new ARRAYLENGTH(owner); break;
				case Opcodes.ASTORE: 			instruction = new ASTORE(owner, reader.readUnsignedByte()); break;
				case Opcodes.ASTORE_0: 		instruction = new ASTORE_0(owner); break;
				case Opcodes.ASTORE_1: 		instruction = new ASTORE_1(owner); break;
				case Opcodes.ASTORE_2: 		instruction = new ASTORE_2(owner); break;
				case Opcodes.ASTORE_3: 		instruction = new ASTORE_3(owner); break;
				case Opcodes.ATHROW: 			instruction = new ATHROW(owner); break;

				case Opcodes.BALOAD: 			instruction = new BALOAD(owner); break;
				case Opcodes.BASTORE: 			instruction = new BASTORE(owner); break;
				case Opcodes.BIPUSH:	 			instruction = new BIPUSH(owner, reader.readByte()); break;
				
				case Opcodes.CALOAD: 			instruction = new CALOAD(owner); break;
				case Opcodes.CASTORE: 			instruction = new CASTORE(owner); break;
				case Opcodes.CHECKCAST:		instruction = new CHECKCAST(owner, (ClassInfo)pool.get(reader.readUnsignedShort())); break;
				
			    case Opcodes.D2F: 					instruction = new D2F(owner); break;
			    case Opcodes.D2I: 					instruction = new D2I(owner); break;
			    case Opcodes.D2L: 					instruction = new D2L(owner); break;
			    case Opcodes.DADD: 				instruction = new DADD(owner); break;
			    case Opcodes.DALOAD: 			instruction = new DALOAD(owner); break;
			    case Opcodes.DASTORE: 			instruction = new DASTORE(owner); break;
			    case Opcodes.DCMPG: 				instruction = new DCMPG(owner); break;
			    case Opcodes.DCMPL: 				instruction = new DCMPL(owner); break;
			    case Opcodes.DCONST_0: 		instruction = new DCONST_0(owner); break;
			    case Opcodes.DCONST_1: 		instruction = new DCONST_1(owner); break;
			    case Opcodes.DDIV: 				instruction = new DDIV(owner); break;
			    case Opcodes.DLOAD: 				instruction = new DLOAD(owner, reader.readUnsignedByte()); break;
			    case Opcodes.DLOAD_0: 			instruction = new DLOAD_0(owner); break;
			    case Opcodes.DLOAD_1: 			instruction = new DLOAD_1(owner); break;
			    case Opcodes.DLOAD_2: 			instruction = new DLOAD_2(owner); break;
			    case Opcodes.DLOAD_3: 			instruction = new DLOAD_3(owner); break;
			    case Opcodes.DMUL: 				instruction = new DMUL(owner); break;
			    case Opcodes.DNEG: 				instruction = new DNEG(owner); break;
			    case Opcodes.DREM: 				instruction = new DREM(owner); break;
			    case Opcodes.DRETURN: 			instruction = new DRETURN(owner); break;
			    case Opcodes.DSTORE: 			instruction = new DSTORE(owner, reader.readUnsignedByte()); break;
			    case Opcodes.DSTORE_0: 		instruction = new DSTORE_0(owner); break;
			    case Opcodes.DSTORE_1: 		instruction = new DSTORE_1(owner); break;
			    case Opcodes.DSTORE_2: 		instruction = new DSTORE_2(owner); break;
			    case Opcodes.DSTORE_3: 		instruction = new DSTORE_3(owner); break;
			    case Opcodes.DSUB: 				instruction = new DSUB(owner); break;
			    case Opcodes.DUP: 					instruction = new DUP(owner); break;
			    case Opcodes.DUP2: 				instruction = new DUP2(owner); break;
			    case Opcodes.DUP2_X1: 			instruction = new DUP2_X1(owner); break;
			    case Opcodes.DUP2_X2: 			instruction = new DUP2_X2(owner); break;
			    case Opcodes.DUP_X1: 			instruction = new DUP_X1(owner); break;
			    case Opcodes.DUP_X2: 			instruction = new DUP_X2(owner); break;

			    case Opcodes.F2D: 					instruction = new F2D(owner); break;
			    case Opcodes.F2I: 					instruction = new F2I(owner); break;
			    case Opcodes.F2L: 					instruction = new F2L(owner); break;
			    case Opcodes.FADD: 				instruction = new FADD(owner); break;
			    case Opcodes.FALOAD: 			instruction = new FALOAD(owner); break;
			    case Opcodes.FASTORE:			instruction = new FASTORE(owner); break;
			    case Opcodes.FCMPG: 				instruction = new FCMPG(owner); break;
			    case Opcodes.FCMPL: 				instruction = new FCMPL(owner); break;
			    case Opcodes.FCONST_0: 		instruction = new FCONST_0(owner); break;
			    case Opcodes.FCONST_1: 		instruction = new FCONST_1(owner); break;
			    case Opcodes.FCONST_2: 		instruction = new FCONST_2(owner); break;
			    case Opcodes.FDIV: 				instruction = new FDIV(owner); break;
			    case Opcodes.FLOAD:				instruction = new FLOAD(owner, reader.readUnsignedByte()); break;
			    case Opcodes.FLOAD_0: 			instruction = new FLOAD_0(owner); break;
			    case Opcodes.FLOAD_1: 			instruction = new FLOAD_1(owner); break;
			    case Opcodes.FLOAD_2: 			instruction = new FLOAD_2(owner); break;
			    case Opcodes.FLOAD_3:			instruction = new FLOAD_3(owner); break;
			    case Opcodes.FMUL: 				instruction = new FMUL(owner); break;
			    case Opcodes.FNEG: 				instruction = new FNEG(owner); break;
			    case Opcodes.FREM: 				instruction = new FREM(owner); break;
			    case Opcodes.FRETURN: 			instruction = new FRETURN(owner); break;
			    case Opcodes.FSTORE: 			instruction = new FSTORE(owner, reader.readUnsignedByte()); break;
			    case Opcodes.FSTORE_0: 			instruction = new FSTORE_0(owner); break;
			    case Opcodes.FSTORE_1: 			instruction = new FSTORE_1(owner); break;
			    case Opcodes.FSTORE_2: 			instruction = new FSTORE_2(owner); break;
			    case Opcodes.FSTORE_3: 			instruction = new FSTORE_3(owner); break;
			    case Opcodes.FSUB: 				instruction = new FSUB(owner); break;
			    
			    case Opcodes.GETFIELD: 			instruction = new GETFIELD(owner, (FieldrefInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.GETSTATIC: 		instruction = new GETSTATIC(owner, (FieldrefInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.GOTO: 				instruction = new GOTO(owner, reader.readShort()); break;
			    case Opcodes.GOTO_W: 			instruction = new GOTO_W(owner, reader.readInt()); break;
			    
			    case Opcodes.I2B: 					instruction = new I2B(owner); break;
			    case Opcodes.I2C: 					instruction = new I2C(owner); break;
			    case Opcodes.I2D: 					instruction = new I2D(owner); break;
			    case Opcodes.I2F: 					instruction = new I2F(owner); break;
			    case Opcodes.I2L: 					instruction = new I2L(owner); break;
			    case Opcodes.I2S: 					instruction = new I2S(owner); break;
			    case Opcodes.IADD: 				instruction = new IADD(owner); break;
			    case Opcodes.IALOAD: 			instruction = new IALOAD(owner); break;
			    case Opcodes.IAND: 				instruction = new IAND(owner); break;
			    case Opcodes.IASTORE: 			instruction = new IASTORE(owner); break;
			    case Opcodes.ICONST_0: 			instruction = new ICONST_0(owner); break;
			    case Opcodes.ICONST_1: 			instruction = new ICONST_1(owner); break;
			    case Opcodes.ICONST_2: 			instruction = new ICONST_2(owner); break;
			    case Opcodes.ICONST_3: 			instruction = new ICONST_3(owner); break;
			    case Opcodes.ICONST_4: 			instruction = new ICONST_4(owner); break;
			    case Opcodes.ICONST_5: 			instruction = new ICONST_5(owner); break;
			    case Opcodes.ICONST_M1: 		instruction = new ICONST_M1(owner); break;
			    case Opcodes.IDIV: 					instruction = new IDIV(owner); break;
			    case Opcodes.IFEQ: 					instruction = new IFEQ(owner, reader.readShort()); break;
			    case Opcodes.IFGE: 					instruction = new IFGE(owner, reader.readShort()); break;
			    case Opcodes.IFGT: 					instruction = new IFGT(owner, reader.readShort()); break;
			    case Opcodes.IFLE: 					instruction = new IFLE(owner, reader.readShort()); break;
			    case Opcodes.IFLT: 					instruction = new IFLT(owner, reader.readShort()); break;
			    case Opcodes.IFNE: 					instruction = new IFNE(owner, reader.readShort()); break;
			    case Opcodes.IFNONNULL: 		instruction = new IFNONNULL(owner, reader.readShort()); break;
			    case Opcodes.IFNULL: 				instruction = new IFNULL(owner, reader.readShort()); break;
			    case Opcodes.IF_ACMPEQ: 		instruction = new IF_ACMPEQ(owner, reader.readShort()); break;
			    case Opcodes.IF_ACMPNE: 		instruction = new IF_ACMPNE(owner, reader.readShort()); break;
			    case Opcodes.IF_ICMPEQ: 			instruction = new IF_ICMPEQ(owner, reader.readShort()); break;
			    case Opcodes.IF_ICMPGE: 			instruction = new IF_ICMPGE(owner, reader.readShort()); break;
			    case Opcodes.IF_ICMPGT: 			instruction = new IF_ICMPGT(owner, reader.readShort()); break;
			    case Opcodes.IF_ICMPLE: 			instruction = new IF_ICMPLE(owner, reader.readShort()); break;
			    case Opcodes.IF_ICMPLT: 			instruction = new IF_ICMPLT(owner, reader.readShort()); break;
			    case Opcodes.IF_ICMPNE: 			instruction = new IF_ICMPNE(owner, reader.readShort()); break;
			    case Opcodes.IINC: 					instruction = new IINC(owner, reader.readUnsignedByte(), reader.readByte()); break;
			    case Opcodes.ILOAD: 				instruction = new ILOAD(owner, reader.readUnsignedByte()); break;
			    case Opcodes.ILOAD_0: 			instruction = new ILOAD_0(owner); break;
			    case Opcodes.ILOAD_1: 			instruction = new ILOAD_1(owner); break;
			    case Opcodes.ILOAD_2: 			instruction = new ILOAD_2(owner); break;
			    case Opcodes.ILOAD_3: 			instruction = new ILOAD_3(owner); break;
			    case Opcodes.IMUL: 				instruction = new IMUL(owner); break;
			    case Opcodes.INEG: 					instruction = new INEG(owner); break;
			    case Opcodes.INSTANCEOF: 		instruction = new INSTANCEOF(owner, (ClassInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.INVOKEINTERFACE: instruction = new INVOKEINTERFACE(owner, (InterfaceMethodrefInfo)pool.get(reader.readUnsignedShort()), reader.readUnsignedByte()); reader.readByte(); break;
			    case Opcodes.INVOKESPECIAL: 	instruction = new INVOKESPECIAL(owner, (MethodrefInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.INVOKESTATIC: 	instruction = new INVOKESTATIC(owner, (MethodrefInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.INVOKEVIRTUAL: 	instruction = new INVOKEVIRTUAL(owner, (MethodrefInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.IOR: 					instruction = new IOR(owner); break;
			    case Opcodes.IREM: 				instruction = new IREM(owner); break;
			    case Opcodes.IRETURN: 			instruction = new IRETURN(owner); break;
			    case Opcodes.ISHL: 					instruction = new ISHL(owner); break;
			    case Opcodes.ISHR: 				instruction = new ISHR(owner); break;
			    case Opcodes.ISTORE: 				instruction = new ISTORE(owner, reader.readUnsignedByte()); break;
			    case Opcodes.ISTORE_0: 			instruction = new ISTORE_0(owner); break;
			    case Opcodes.ISTORE_1: 			instruction = new ISTORE_1(owner); break;
			    case Opcodes.ISTORE_2: 			instruction = new ISTORE_2(owner); break;
			    case Opcodes.ISTORE_3: 			instruction = new ISTORE_3(owner); break;
			    case Opcodes.ISUB: 					instruction = new ISUB(owner); break;
			    case Opcodes.IUSHR: 				instruction = new IUSHR(owner); break;
			    case Opcodes.IXOR: 				instruction = new IXOR(owner); break;
			    
			    case Opcodes.JSR: 					instruction = new JSR(owner, reader.readShort()); break;
			    case Opcodes.JSR_W: 				instruction = new JSR_W(owner, reader.readInt()); break;
			    
			    case Opcodes.L2D: 					instruction = new L2D(owner); break;
			    case Opcodes.L2F: 					instruction = new L2F(owner); break;
			    case Opcodes.L2I: 					instruction = new L2I(owner); break;
			    case Opcodes.LADD: 				instruction = new LADD(owner); break;
			    case Opcodes.LALOAD: 			instruction = new LALOAD(owner); break;
			    case Opcodes.LAND: 				instruction = new LAND(owner); break;
			    case Opcodes.LASTORE:			instruction = new LASTORE(owner); break;
			    case Opcodes.LCMP: 				instruction = new LCMP(owner); break;
			    case Opcodes.LCONST_0: 		instruction = new LCONST_0(owner); break;
			    case Opcodes.LCONST_1: 		instruction = new LCONST_1(owner); break;
			    case Opcodes.LDC: 					instruction = new LDC(owner, pool.get(reader.readUnsignedByte())); break;
			    case Opcodes.LDC2_W: 			instruction = new LDC2_W(owner, pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.LDC_W: 				instruction = new LDC_W(owner, pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.LDIV: 				instruction = new LDIV(owner); break;
			    case Opcodes.LLOAD: 				instruction = new LLOAD(owner, reader.readUnsignedByte()); break;
			    case Opcodes.LLOAD_0: 			instruction = new LLOAD_0(owner); break;
			    case Opcodes.LLOAD_1: 			instruction = new LLOAD_1(owner); break;
			    case Opcodes.LLOAD_2: 			instruction = new LLOAD_2(owner); break;
			    case Opcodes.LLOAD_3: 			instruction = new LLOAD_3(owner); break;
			    case Opcodes.LMUL: 				instruction = new LMUL(owner); break;
			    case Opcodes.LNEG: 				instruction = new LNEG(owner); break;
			    case Opcodes.LOOKUPSWITCH: 	

			    	// Read zeroed bytes so that the default is a multiple of four from the start of this code array.
			    	remainder = (byteIndex + 1) % 4;
			    	padding = remainder == 0 ? 0 : 4 - remainder;
			    	for(int i = 0; i < padding; i++) reader.readUnsignedByte();

			    	// Read the default bytes
			    	int defaultOffset = reader.readInt();
			    	// Read the number of pairs
			    	int numberOfPairs = reader.readInt();
			    	
			    	// Read all of the pairs
			    	instruction = new LOOKUPSWITCH(owner, defaultOffset, numberOfPairs);
			    	for(int i = 0; i < numberOfPairs; i++)
			    		((LOOKUPSWITCH)instruction).setPair(i, reader.readInt(), reader.readInt());
			    	
			    	byteLength = 1 + padding + 4 + 4 + numberOfPairs * 8;
			    	
			    	break;
			    	
			    case Opcodes.LOR: 					instruction = new LOR(owner); break;
			    case Opcodes.LREM: 				instruction = new LREM(owner); break;
			    case Opcodes.LRETURN: 			instruction = new LRETURN(owner); break;
			    case Opcodes.LSHL: 				instruction = new LSHL(owner); break;
			    case Opcodes.LSHR: 				instruction = new LSHR(owner); break;
			    case Opcodes.LSTORE: 			instruction = new LSTORE(owner, reader.readUnsignedByte()); break;
			    case Opcodes.LSTORE_0: 			instruction = new LSTORE_0(owner); break;
			    case Opcodes.LSTORE_1: 			instruction = new LSTORE_1(owner); break;
			    case Opcodes.LSTORE_2: 			instruction = new LSTORE_2(owner); break;
			    case Opcodes.LSTORE_3: 			instruction = new LSTORE_3(owner); break;
			    case Opcodes.LSUB: 				instruction = new LSUB(owner); break;
			    case Opcodes.LUSHR: 				instruction = new LUSHR(owner); break;
			    case Opcodes.LXOR: 				instruction = new LXOR(owner); break;
			    
			    case Opcodes.MONITORENTER: 	instruction = new MONITORENTER(owner); break;
			    case Opcodes.MONITOREXIT: 	instruction = new MONITOREXIT(owner); break;
			    case Opcodes.MULTIANEWARRAY: instruction = new MULTIANEWARRAY(owner, (ClassInfo)pool.get(reader.readUnsignedShort()), reader.readUnsignedByte()); break;
			    
			    case Opcodes.NEW: 					instruction = new NEW(owner, (ClassInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.NEWARRAY: 		instruction = new NEWARRAY(owner, reader.readUnsignedByte()); break;
			    case Opcodes.NOP: 					instruction = new NOP(owner); break;
			    
			    case Opcodes.POP: 					instruction = new POP(owner); break;
			    case Opcodes.POP2: 				instruction = new POP2(owner); break;
			    case Opcodes.PUTFIELD: 			instruction = new PUTFIELD(owner, (FieldrefInfo)pool.get(reader.readUnsignedShort())); break;
			    case Opcodes.PUTSTATIC: 		instruction = new PUTSTATIC(owner, (FieldrefInfo)pool.get(reader.readUnsignedShort())); break;
			    
			    case Opcodes.RET: 					instruction = new RET(owner, reader.readUnsignedByte()); break;
			    case Opcodes.RETURN: 			instruction = new RETURN(owner); break;
			    
			    case Opcodes.SALOAD: 			instruction = new SALOAD(owner); break;
			    case Opcodes.SASTORE: 			instruction = new SASTORE(owner); break;
			    case Opcodes.SIPUSH: 				instruction = new SIPUSH(owner, reader.readShort()); break;
			    case Opcodes.SWAP: 				instruction = new SWAP(owner); break;

			    case Opcodes.TABLESWITCH: 	
			    	
			    	// Read zeroed bytes so that the default is a multiple of four from the start of this code array.
			    	remainder = (byteIndex + 1) % 4;
			    	padding = remainder == 0 ? 0 : 4 - remainder;
			    	for(int i = 0; i < padding; i++) reader.readUnsignedByte();

			    	// Read the default bytes
			    	int def = reader.readInt();
			    	// Read the number of pairs
			    	int low = reader.readInt();
			    	int high = reader.readInt();
			    	int number = high - low + 1;
			    	// Read all of the offsets
			    	ArrayList<Integer> offsets = new ArrayList<Integer>(number);
			    	for(int i = 0; i < high - low + 1; i++) offsets.add(reader.readInt());
			    	instruction = new TABLESWITCH(owner, def, low, high, offsets);
			    	
					byteLength = 1 + padding + 4 + 4 + 4 + (high - low + 1) * 4;
			    	
			    	break;
			    
			    case Opcodes.WIDE:
			    	int nextOpcode = reader.readUnsignedByte(); 
			    	int localID = reader.readUnsignedByte() << 8 | reader.readUnsignedByte();
			    	instruction = new WIDE(owner, nextOpcode, localID, nextOpcode == Opcodes.IINC ? reader.readShort() : 0);
			    	break;

				default: assert false : "Couldn't find an opcode for opcode value " + opcode + " in code for " + owner.getMethod() + " of class " + owner.getMethod().getInternalName();
			
			}
			
			// If we assigned the byte length above (for a TABLESWITCH or LOOKUPSWITCH), then we don't
			// compute the instruction length here because it depends on the byte index of the instruction, which is not 
			// yet set. Otherwise, its constant and we can determine it here.
			if(byteLength == 0)
				byteLength = instruction.byteLength();
			
			bytesRemaining -= byteLength;
			
			instructions[index] = instruction;
			index++;
			
		}
		
		Instruction[] array = new Instruction[index];
		System.arraycopy(instructions, 0, array, 0, index);
		return array;
		
	}
	
}