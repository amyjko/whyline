package edu.cmu.hcii.whyline.bytecode;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class Opcodes {

    public static final int AALOAD = 50;
    public static final int AASTORE = 83;
    public static final int ACONST_NULL = 1;
    public static final int ALOAD = 25;
    public static final int ALOAD_0 = 42;
    public static final int ALOAD_1 = 43;
    public static final int ALOAD_2 = 44;
    public static final int ALOAD_3 = 45;
    public static final int ANEWARRAY = 189;
    public static final int ARETURN = 176;
    public static final int ARRAYLENGTH = 190;
    public static final int ASTORE = 58;
    public static final int ASTORE_0 = 75;
    public static final int ASTORE_1 = 76;
    public static final int ASTORE_2 = 77;
    public static final int ASTORE_3 = 78;
    public static final int ATHROW = 191;
    public static final int BALOAD = 51;
    public static final int BASTORE = 84;
    public static final int BIPUSH = 16;
    public static final int CALOAD = 52;
    public static final int CASTORE = 85;
    public static final int CHECKCAST = 192;
    public static final int D2F = 144;
    public static final int D2I = 142;
    public static final int D2L = 143;
    public static final int DADD = 99;
    public static final int DALOAD = 49;
    public static final int DASTORE = 82;
    public static final int DCMPG = 152;
    public static final int DCMPL = 151;
    public static final int DCONST_0 = 14;
    public static final int DCONST_1 = 15;
    public static final int DDIV = 111;
    public static final int DLOAD = 24;
    public static final int DLOAD_0 = 38;
    public static final int DLOAD_1 = 39;
    public static final int DLOAD_2 = 40;
    public static final int DLOAD_3 = 41;
    public static final int DMUL = 107;
    public static final int DNEG = 119;
    public static final int DREM = 115;
    public static final int DRETURN = 175;
    public static final int DSTORE = 57;
    public static final int DSTORE_0 = 71;
    public static final int DSTORE_1 = 72;
    public static final int DSTORE_2 = 73;
    public static final int DSTORE_3 = 74;
    public static final int DSUB = 103;
    public static final int DUP = 89;
    public static final int DUP2 = 92;
    public static final int DUP2_X1 = 93;
    public static final int DUP2_X2 = 94;
    public static final int DUP_X1 = 90;
    public static final int DUP_X2 = 91;
    public static final int F2D = 141;
    public static final int F2I = 139;
    public static final int F2L = 140;
    public static final int FADD = 98;
    public static final int FALOAD = 48;
    public static final int FASTORE = 81;
    public static final int FCMPG = 150;
    public static final int FCMPL = 149;
    public static final int FCONST_0 = 11;
    public static final int FCONST_1 = 12;
    public static final int FCONST_2 = 13;
    public static final int FDIV = 110;
    public static final int FLOAD = 23;
    public static final int FLOAD_0 = 34;
    public static final int FLOAD_1 = 35;
    public static final int FLOAD_2 = 36;
    public static final int FLOAD_3 = 37;
    public static final int FMUL = 106;
    public static final int FNEG = 118;
    public static final int FREM = 114;
    public static final int FRETURN = 174;
    public static final int FSTORE = 56;
    public static final int FSTORE_0 = 67;
    public static final int FSTORE_1 = 68;
    public static final int FSTORE_2 = 69;
    public static final int FSTORE_3 = 70;
    public static final int FSUB = 102;
    public static final int GETFIELD = 180;
    public static final int GETSTATIC = 178;
    public static final int GOTO = 167;
    public static final int GOTO_W = 200;
    public static final int I2B = 145;
    public static final int I2C = 146;
    public static final int I2D = 135;
    public static final int I2F = 134;
    public static final int I2L = 133;
    public static final int I2S = 147;
    public static final int IADD = 96;
    public static final int IALOAD = 46;
    public static final int IAND = 126;
    public static final int IASTORE = 79;
    public static final int ICONST_0 = 3;
    public static final int ICONST_1 = 4;
    public static final int ICONST_2 = 5;
    public static final int ICONST_3 = 6;
    public static final int ICONST_4 = 7;
    public static final int ICONST_5 = 8;
    public static final int ICONST_M1 = 2;
    public static final int IDIV = 108;
    public static final int IFEQ = 153;
    public static final int IFGE = 156;
    public static final int IFGT = 157;
    public static final int IFLE = 158;
    public static final int IFLT = 155;
    public static final int IFNE = 154;
    public static final int IFNONNULL = 199;
    public static final int IFNULL = 198;
    public static final int IF_ACMPEQ = 165;
    public static final int IF_ACMPNE = 166;
    public static final int IF_ICMPEQ = 159;
    public static final int IF_ICMPGE = 162;
    public static final int IF_ICMPGT = 163;
    public static final int IF_ICMPLE = 164;
    public static final int IF_ICMPLT = 161;
    public static final int IF_ICMPNE = 160;
    public static final int IINC = 132;
    public static final int ILOAD = 21;
    public static final int ILOAD_0 = 26;
    public static final int ILOAD_1 = 27;
    public static final int ILOAD_2 = 28;
    public static final int ILOAD_3 = 29;
    public static final int IMUL = 104;
    public static final int INEG = 116;
    public static final int INSTANCEOF = 193;
    public static final int INVOKEINTERFACE = 185;
    public static final int INVOKESPECIAL = 183;
    public static final int INVOKESTATIC = 184;
    public static final int INVOKEVIRTUAL = 182;
    public static final int IOR = 128;
    public static final int IREM = 112;
    public static final int IRETURN = 172;
    public static final int ISHL = 120;
    public static final int ISHR = 122;
    public static final int ISTORE = 54;
    public static final int ISTORE_0 = 59;
    public static final int ISTORE_1 = 60;
    public static final int ISTORE_2 = 61;
    public static final int ISTORE_3 = 62;
    public static final int ISUB = 100;
    public static final int IUSHR = 124;
    public static final int IXOR = 130;
    public static final int JSR = 168;
    public static final int JSR_W = 201;
    public static final int L2D = 138;
    public static final int L2F = 137;
    public static final int L2I = 136;
    public static final int LADD = 97;
    public static final int LALOAD = 47;
    public static final int LAND = 127;
    public static final int LASTORE = 80;
    public static final int LCMP = 148;
    public static final int LCONST_0 = 9;
    public static final int LCONST_1 = 10;
    public static final int LDC = 18;
    public static final int LDC2_W = 20;
    public static final int LDC_W = 19;
    public static final int LDIV = 109;
    public static final int LLOAD = 22;
    public static final int LLOAD_0 = 30;
    public static final int LLOAD_1 = 31;
    public static final int LLOAD_2 = 32;
    public static final int LLOAD_3 = 33;
    public static final int LMUL = 105;
    public static final int LNEG = 117;
    public static final int LOOKUPSWITCH = 171;
    public static final int LOR = 129;
    public static final int LREM = 113;
    public static final int LRETURN = 173;
    public static final int LSHL = 121;
    public static final int LSHR = 123;
    public static final int LSTORE = 55;
    public static final int LSTORE_0 = 63;
    public static final int LSTORE_1 = 64;
    public static final int LSTORE_2 = 65;
    public static final int LSTORE_3 = 66;
    public static final int LSUB = 101;
    public static final int LUSHR = 125;
    public static final int LXOR = 131;
    public static final int MONITORENTER = 194;
    public static final int MONITOREXIT = 195;
    public static final int MULTIANEWARRAY = 197;
    public static final int NEW = 187;
    public static final int NEWARRAY = 188;
    public static final int NOP = 0;
    public static final int POP = 87;
    public static final int POP2 = 88;
    public static final int PUTFIELD = 181;
    public static final int PUTSTATIC = 179;
    public static final int RET = 169;
    public static final int RETURN = 177;
    public static final int SALOAD = 53;
    public static final int SASTORE = 86;
    public static final int SIPUSH = 17;
    public static final int SWAP = 95;
    public static final int TABLESWITCH = 170;
    public static final int WIDE = 196;
	    
	public static final int POPS_ALL_OPERANDS = -1;

    // Used to determine whether to trace the instruction, or just simulate it. An instruction is
    // constant if it takes no operands and always produces the same operand, or produces no operand.
    public static final boolean[] EXECUTION_IS_VARIABLE = {
    	
    	false, // nop, 0
    	false, // aconst_null, 1
        false, // iconst_m1, 2
        false, // iconst_0, 3
        false, // iconst_1, 4
        false, // iconst_2, 5
        false, // iconst_3, 6
        false, // iconst_4, 7
        false, // iconst_5, 8
        false, // lconst_0, 9
        false, // lconst_1, 10
        false, // fconst_0, 11
        false, // fconst_1, 12
        false, // fconst_2, 13
        false, // dconst_0, 14
        false, // dconst_1, 15
        false, // bipush, 16
        false, // sipush, 17
        false, // ldc, 18
        false, // ldc_w, 19
        false, // ldc2_w, 20
        
        true, // iload, 21
        true, // lload, 22
        true, // fload, 23
        true, // dload, 24
        true, // aload, 25
        true, // iload_0, 26
        true, // iload_1, 27
        true, // iload_2, 28
        true, // iload_3, 29
        true, // lload_0, 30
        true, // lload_1, 31
        true, // lload_2, 32
        true, // lload_3, 33
        true, // fload_0, 34
        true, // fload_1, 35
        true, // fload_2, 36
        true, // fload_3, 37
        true, // dload_0, 38
        true, // dload_1, 39
        true, // dload_2, 40
        true, // dload_3, 41
        true, // aload_0, 42
        true, // aload_1, 43
        true, // aload_2, 44
        true, // aload_3, 45
        
        // Consume an array reference and index and produce a value
        true, // iaload, 46
        true, // laload, 47
        true, // faload, 48
        true, // daload, 49
        true, // aaload, 50
        true, // baload, 51
        true, // caload, 52
        true, // saload, 53

        // Consume one or two operands and store it in a local
        false, // istore, 54
        false, // lstore, 55
        false, // fstore, 56
        false, // dstore, 57
        false, // astore, 58
        false, // istore_0, 59
        false, // istore_1, 60
        false, // istore_2, 61
        false, // istore_3, 62
        false, // lstore_0, 63
        false, // lstore_1, 64
        false, // lstore_2, 65
        false, // lstore_3, 66
        false, // fstore_0, 67
        false, // fstore_false, 68
        false, // fstore_2, 69
        false, // fstore_3, 70
        false, // dstore_0, 71
        false, // dstore_1, 72
        false, // dstore_2, 73
        false, // dstore_3, 74
        false, // astore_0, 75
        false, // astore_1, 76
        false, // astore_2, 77
        false, // astore_3, 78
        
        // Consume an array reference, index and value
        false, // iastore, 79
        false, // lastore, 80
        false, // fastore, 81
        false, // dastore, 82
        false, // aastore, 83
        false, // bastore, 84
        false, // castore, 85
        false, // sastore, 86

        // Consume values
        false, // pop, 87
        false, // pop2, 88
        
        // Duplicate values
        false, // dup, 89
        false, // dup_x1, 90
        false, // dup_x2, 91
        false, // dup2, 92
        false, // dup2_x1, 93
        false, // dup2_x2, 94
        
        // Swap
        false, // swap, 95

        // Consume a few values to produce one.
        false, // iadd, 96
        false, // ladd, 97
        false, // fadd, 98
        false, // dadd, 99
        false, // isub, 100
        false, // lsub, 101
        false, // fsub, 102
        false, // dsub, 103
        false, // imul, 104
        false, // lmul, 105
        false, // fmul, 106
        false, // dmul, 107
        false, // idiv, 108
        false, // ldiv, 109
        false, // fdiv, 110
        false, // ddiv, 111
        false, // irem, 112
        false, // lrem, 113
        false, // frem, 114
        false, // drem, 115
        false, // ineg, 116
        false, // lneg, 117
        false, // fneg, 118
        false, // dneg, 119
        false, // ishl, 120
        false, // lshl, 121
        false, // ishr, 122
        false, // lshr, 123
        false, // iushr, 124
        false, // lushr, 125
        false, // iand, 126
        false, // land, 127
        false, // ior, 128
        false, // lor, 129
        false, // ixor, 130
        false, // lxor, 131
        false, // iinc, 132
        false, // i2l, 133
        false, // i2f, 134
        false, // i2d, 135
        false, // l2i, 136
        false, // l2f, 137
        false, // l2d, 138
        false, // f2i, 139
        false, // f2l, 140
        false, // f2d, 141
        false, // d2i, 142
        false, // d2l, 143
        false, // d2f, 144
        false, // i2b, 145
        false, // i2c, 146
        false, // i2s, 147
        
        false, // lcmp, 148
        false, // fcmpl, 149
        false, // fcmpg, 150
        false, // dcmpl, 151
        false, // dcmpg, 152
        false, // ifeq, 153
        false, // ifne, 154
        false, // iflt, 155
        false, // ifge, 156
        false, // ifgt, 157
        false, // ifle, 158
        false, // if_icmpeq, 159
        false, // if_icmpne, 160
        false, // if_icmplt, 161
        false, // if_icmpge, 162
        false, // if_icmpgt, 163
        false, // if_icmple, 164
        false, // if_acmpeq, 165
        false, // if_acmpne, 166
        false, // goto, 167
        false, // jsr, 168
        false, // ret, 169
        false, // tableswitch, 170
        false, // lookupswitch, 171
        false, // ireturn, 172
        false, // lreturn, 173
        false, // freturn, 174
        false, // dreturn, 175
        false, // areturn, 176
        false, // return, 177
        true, // getstatic, 178            
        true, // putstatic, 179            
        true, // getfield, 180             
        true, // putfield, 181             
        true, // invokevirtual, 182       
        true, // invokespecial, 183       
        true, // invokestatic, 184        
        true, // invokeinterface, 185    
       	false, // undefined, 186
        false, // new, 187
        false, // newarray, 188
        false, // anewarray, 189
        false, // arraylength, 190
        false, // athrow, 191               stack is cleared
        false, // checkcast, 192
        false, // instanceof, 193
       	false, // monitorenter, 194
       	false, // monitorexit, 195
        false, // wide, 196                 depends on the following opcode
        false, // multianewarray, 197       depends on the dimensions
        false, // ifnull, 198
        false, // ifnonnull, 199
        false, // goto_w, 200
        false // jsr_w, 201
    };

    public static final String[] NAMES = {
            "nop",  /* 0*/
            "aconst_null",  /* 1*/
            "iconst_m1",    /* 2*/
            "iconst_0",     /* 3*/
            "iconst_1",     /* 4*/
            "iconst_2",     /* 5*/
            "iconst_3",     /* 6*/
            "iconst_4",     /* 7*/
            "iconst_5",     /* 8*/
            "lconst_0",     /* 9*/
            "lconst_1",     /* 10*/
            "fconst_0",     /* 11*/
            "fconst_1",     /* 12*/
            "fconst_2",     /* 13*/
            "dconst_0",     /* 14*/
            "dconst_1",     /* 15*/
            "bipush",       /* 16*/
            "sipush",       /* 17*/
            "ldc",  /* 18*/
            "ldc_w",        /* 19*/
            "ldc2_w",       /* 20*/
            "iload",        /* 21*/
            "lload",        /* 22*/
            "fload",        /* 23*/
            "dload",        /* 24*/
            "aload",        /* 25*/
            "iload_0",      /* 26*/
            "iload_1",      /* 27*/
            "iload_2",      /* 28*/
            "iload_3",      /* 29*/
            "lload_0",      /* 30*/
            "lload_1",      /* 31*/
            "lload_2",      /* 32*/
            "lload_3",      /* 33*/
            "fload_0",      /* 34*/
            "fload_1",      /* 35*/
            "fload_2",      /* 36*/
            "fload_3",      /* 37*/
            "dload_0",      /* 38*/
            "dload_1",      /* 39*/
            "dload_2",      /* 40*/
            "dload_3",      /* 41*/
            "aload_0",      /* 42*/
            "aload_1",      /* 43*/
            "aload_2",      /* 44*/
            "aload_3",      /* 45*/
            "iaload",       /* 46*/
            "laload",       /* 47*/
            "faload",       /* 48*/
            "daload",       /* 49*/
            "aaload",       /* 50*/
            "baload",       /* 51*/
            "caload",       /* 52*/
            "saload",       /* 53*/
            "istore",       /* 54*/
            "lstore",       /* 55*/
            "fstore",       /* 56*/
            "dstore",       /* 57*/
            "astore",       /* 58*/
            "istore_0",     /* 59*/
            "istore_1",     /* 60*/
            "istore_2",     /* 61*/
            "istore_3",     /* 62*/
            "lstore_0",     /* 63*/
            "lstore_1",     /* 64*/
            "lstore_2",     /* 65*/
            "lstore_3",     /* 66*/
            "fstore_0",     /* 67*/
            "fstore_1",     /* 68*/
            "fstore_2",     /* 69*/
            "fstore_3",     /* 70*/
            "dstore_0",     /* 71*/
            "dstore_1",     /* 72*/
            "dstore_2",     /* 73*/
            "dstore_3",     /* 74*/
            "astore_0",     /* 75*/
            "astore_1",     /* 76*/
            "astore_2",     /* 77*/
            "astore_3",     /* 78*/
            "iastore",      /* 79*/
            "lastore",      /* 80*/
            "fastore",      /* 81*/
            "dastore",      /* 82*/
            "aastore",      /* 83*/
            "bastore",      /* 84*/
            "castore",      /* 85*/
            "sastore",      /* 86*/
            "pop",  /* 87*/
            "pop2", /* 88*/
            "dup",  /* 89*/
            "dup_x1",       /* 90*/
            "dup_x2",       /* 91*/
            "dup2", /* 92*/
            "dup2_x1",      /* 93*/
            "dup2_x2",      /* 94*/
            "swap", /* 95*/
            "iadd", /* 96*/
            "ladd", /* 97*/
            "fadd", /* 98*/
            "dadd", /* 99*/
            "isub", /* 100*/
            "lsub", /* 101*/
            "fsub", /* 102*/
            "dsub", /* 103*/
            "imul", /* 104*/
            "lmul", /* 105*/
            "fmul", /* 106*/
            "dmul", /* 107*/
            "idiv", /* 108*/
            "ldiv", /* 109*/
            "fdiv", /* 110*/
            "ddiv", /* 111*/
            "irem", /* 112*/
            "lrem", /* 113*/
            "frem", /* 114*/
            "drem", /* 115*/
            "ineg", /* 116*/
            "lneg", /* 117*/
            "fneg", /* 118*/
            "dneg", /* 119*/
            "ishl", /* 120*/
            "lshl", /* 121*/
            "ishr", /* 122*/
            "lshr", /* 123*/
            "iushr",        /* 124*/
            "lushr",        /* 125*/
            "iand", /* 126*/
            "land", /* 127*/
            "ior",  /* 128*/
            "lor",  /* 129*/
            "ixor", /* 130*/
            "lxor", /* 131*/
            "iinc", /* 132*/
            "i2l",  /* 133*/
            "i2f",  /* 134*/
            "i2d",  /* 135*/
            "l2i",  /* 136*/
            "l2f",  /* 137*/
            "l2d",  /* 138*/
            "f2i",  /* 139*/
            "f2l",  /* 140*/
            "f2d",  /* 141*/
            "d2i",  /* 142*/
            "d2l",  /* 143*/
            "d2f",  /* 144*/
            "i2b",  /* 145*/
            "i2c",  /* 146*/
            "i2s",  /* 147*/
            "lcmp", /* 148*/
            "fcmpl",        /* 149*/
            "fcmpg",        /* 150*/
            "dcmpl",        /* 151*/
            "dcmpg",        /* 152*/
            "ifeq", /* 153*/
            "ifne", /* 154*/
            "iflt", /* 155*/
            "ifge", /* 156*/
            "ifgt", /* 157*/
            "ifle", /* 158*/
            "if_icmpeq",    /* 159*/
            "if_icmpne",    /* 160*/
            "if_icmplt",    /* 161*/
            "if_icmpge",    /* 162*/
            "if_icmpgt",    /* 163*/
            "if_icmple",    /* 164*/
            "if_acmpeq",    /* 165*/
            "if_acmpne",    /* 166*/
            "goto", /* 167*/
            "jsr",  /* 168*/
            "ret",  /* 169*/
            "tableswitch",  /* 170*/
            "lookupswitch", /* 171*/
            "ireturn",      /* 172*/
            "lreturn",      /* 173*/
            "freturn",      /* 174*/
            "dreturn",      /* 175*/
            "areturn",      /* 176*/
            "return",       /* 177*/
            "getstatic",    /* 178*/
            "putstatic",    /* 179*/
            "getfield",     /* 180*/
            "putfield",     /* 181*/
            "invokevirtual",        /* 182*/
            "invokespecial",        /* 183*/
            "invokestatic", /* 184*/
            "invokeinterface",      /* 185*/
            null,
            "new",  /* 187*/
            "newarray",     /* 188*/
            "anewarray",    /* 189*/
            "arraylength",  /* 190*/
            "athrow",       /* 191*/
            "checkcast",    /* 192*/
            "instanceof",   /* 193*/
            "monitorenter", /* 194*/
            "monitorexit",  /* 195*/
            "wide", /* 196*/
            "multianewarray",       /* 197*/
            "ifnull",       /* 198*/
            "ifnonnull",    /* 199*/
            "goto_w",       /* 200*/
            "jsr_w",         /* 201*/
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
            "not an opcode",
    };

}
