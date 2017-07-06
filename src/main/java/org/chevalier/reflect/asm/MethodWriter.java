/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chevalier.reflect.asm;

/**
 * A {@link MethodVisitor} that generates methods in bytecode form. Each visit
 * method of this class appends the bytecode corresponding to the visited
 * instruction to a byte vector, in the order these methods are called.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class MethodWriter extends MethodVisitor {

    /**
     * Pseudo access flag used to denote constructors.
     */
    static final int ACC_CONSTRUCTOR = 0x80000;

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is zero.
     */
    static final int SAME_FRAME = 0; // to 63 (0-3f)

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is 1
     */
    static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)

    /**
     * Reserved for future use
     */
    static final int RESERVED = 128;

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is 1. Offset is bigger then 63;
     */
    static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7

    /**
     * Frame where current locals are the same as the locals in the previous
     * frame, except that the k last locals are absent. The value of k is given
     * by the formula 251-frame_type.
     */
    static final int CHOP_FRAME = 248; // to 250 (f8-fA)

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is zero. Offset is bigger then 63;
     */
    static final int SAME_FRAME_EXTENDED = 251; // fb

    /**
     * Frame where current locals are the same as the locals in the previous
     * frame, except that k additional locals are defined. The value of k is
     * given by the formula frame_type-251.
     */
    static final int APPEND_FRAME = 252; // to 254 // fc-fe

    /**
     * Full frame
     */
    static final int FULL_FRAME = 255; // ff

    /**
     * The class writer to which this method must be added.
     */
    final ClassWriter cw;

    /**
     * Access flags of this method.
     */
    private int access;

    /**
     * The index of the constant pool item that contains the name of this
     * method.
     */
    private final int name;

    /**
     * The index of the constant pool item that contains the descriptor of this
     * method.
     */
    private final int desc;

    /**
     * The descriptor of this method.
     */
    private final String descriptor;

    /**
     * Number of exceptions that can be thrown by this method.
     */
    int exceptionCount;

    /**
     * The exceptions that can be thrown by this method. More precisely, this
     * array contains the indexes of the constant pool items that contain the
     * internal names of these exception classes.
     */
    int[] exceptions;

    /**
     * The bytecode of this method.
     */
    private ByteVector code = new ByteVector();

    /**
     * Maximum stack size of this method.
     */
    private int maxStack;

    /**
     * Maximum number of local variables for this method.
     */
    private int maxLocals;

    /**
     * Number of local variables in the current stack map frame.
     */
    private int currentLocals;

    /**
     * Number of stack map frames in the StackMapTable attribute.
     */
    private int frameCount;

    /**
     * The StackMapTable attribute.
     */
    private ByteVector stackMap;

    /**
     * The offset of the last frame that was written in the StackMapTable
     * attribute.
     */
    private int previousFrameOffset;

    /**
     * The last frame that was written in the StackMapTable attribute.
     * 
     * @see #frame
     */
    private int[] previousFrame;

    /**
     * The current stack map frame. The first element contains the offset of the
     * instruction to which the frame corresponds, the second element is the
     * number of locals and the third one is the number of stack elements. The
     * local variables start at index 3 and are followed by the operand stack
     * values. In summary frame[0] = offset, frame[1] = nLocal, frame[2] =
     * nStack, frame[3] = nLocal. All types are encoded as integers, with the
     * same format as the one used in {@link Label}, but limited to BASE types.
     */
    private int[] frame;

    /**
     * Number of elements in the exception handler list.
     */
    private int handlerCount;

    /**
     * Number of entries in the MethodParameters attribute.
     */
    private int methodParametersCount;

    /**
     * The MethodParameters attribute.
     */
    private ByteVector methodParameters;

    /**
     * Number of entries in the LocalVariableTable attribute.
     */
    private int localVarCount;

    /**
     * The LocalVariableTable attribute.
     */
    private ByteVector localVar;

    /**
     * Number of entries in the LocalVariableTypeTable attribute.
     */
    private int localVarTypeCount;

    /**
     * The LocalVariableTypeTable attribute.
     */
    private ByteVector localVarType;

    /**
     * Number of entries in the LineNumberTable attribute.
     */
    private int lineNumberCount;

    /**
     * The LineNumberTable attribute.
     */
    private ByteVector lineNumber;

    // ------------------------------------------------------------------------

    /*
     * Fields for the control flow graph analysis algorithm (used to compute the
     * maximum stack size). A control flow graph contains one node per "basic
     * block", and one edge per "jump" from one basic block to another. Each
     * node (i.e., each basic block) is represented by the Label object that
     * corresponds to the first instruction of this basic block. Each node also
     * stores the list of its successors in the graph, as a linked list of Edge
     * objects.
     */

    /**
     * A list of labels. This list is the list of basic blocks in the method,
     * i.e. a list of Label objects linked to each other by their
     * {@link Label#successor} field, in the order they are visited by
     * {@link MethodVisitor#visitLabel}, and starting with the first basic
     * block.
     */
    private Label labels;

    /**
     * The previous basic block.
     */
    private Label previousBlock;

    /**
     * The current basic block.
     */
    private Label currentBlock;

    /**
     * The (relative) stack size after the last visited instruction. This size
     * is relative to the beginning of the current basic block, i.e., the true
     * stack size after the last visited instruction is equal to the
     * {@link Label#inputStackTop beginStackSize} of the current basic block
     * plus <tt>stackSize</tt>.
     */
    private int stackSize;

    /**
     * The (relative) maximum stack size after the last visited instruction.
     * This size is relative to the beginning of the current basic block, i.e.,
     * the true maximum stack size after the last visited instruction is equal
     * to the {@link Label#inputStackTop beginStackSize} of the current basic
     * block plus <tt>stackSize</tt>.
     */
    private int maxStackSize;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructs a new {@link MethodWriter}.
     * 
     * @param cw
     *            the class writer in which the method must be added.
     * @param access
     *            the method's access flags (see {@link Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link Type}).
     * @param signature
     *            the method's signature. May be <tt>null</tt>.
     * @param exceptions
     *            the internal names of the method's exceptions. May be
     *            <tt>null</tt>.
     * @param computeMaxs
     *            <tt>true</tt> if the maximum stack size and number of local
     *            variables must be automatically computed.
     * @param computeFrames
     *            <tt>true</tt> if the stack map tables must be recomputed from
     *            scratch.
     */
    MethodWriter(final ClassWriter cw, final int access, final String name,
            final String desc, final String signature,
            final String[] exceptions, final boolean computeMaxs,
            final boolean computeFrames) {
        super(Opcodes.ASM5);
        if (cw.firstMethod == null) {
            cw.firstMethod = this;
        } else {
            cw.lastMethod.mv = this;
        }
        cw.lastMethod = this;
        this.cw = cw;
        this.access = access;
        if ("<init>".equals(name)) {
            this.access |= ACC_CONSTRUCTOR;
        }
        this.name = cw.newUTF8(name);
        this.desc = cw.newUTF8(desc);
        this.descriptor = desc;
        if (exceptions != null && exceptions.length > 0) {
            exceptionCount = exceptions.length;
            this.exceptions = new int[exceptionCount];
            for (int i = 0; i < exceptionCount; ++i) {
                this.exceptions[i] = cw.newClass(exceptions[i]);
            }
        }
        
        if (computeMaxs || computeFrames) {
            // updates maxLocals
            int size = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
            if ((access & Opcodes.ACC_STATIC) != 0) {
                --size;
            }
            maxLocals = size;
            currentLocals = size;
            // creates and visits the label for the first basic block
            labels = new Label();
            labels.status |= Label.PUSHED;
            visitLabel(labels);
        }
    }

    // ------------------------------------------------------------------------
    // Implementation of the MethodVisitor abstract class
    // ------------------------------------------------------------------------

    @Override
    public void visitParameter(String name, int access) {
        if (methodParameters == null) {
            methodParameters = new ByteVector();
        }
        ++methodParametersCount;
        methodParameters.putShort((name == null) ? 0 : cw.newUTF8(name))
                .putShort(access);
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {

        if (type == Opcodes.F_NEW) {
            if (previousFrame == null) {
                visitImplicitFirstFrame();
            }
            currentLocals = nLocal;
            int frameIndex = startFrame(code.length, nLocal, nStack);
            for (int i = 0; i < nLocal; ++i) {
                if (local[i] instanceof String) {
                    frame[frameIndex++] = Frame.OBJECT
                            | cw.addType((String) local[i]);
                } else if (local[i] instanceof Integer) {
                    frame[frameIndex++] = ((Integer) local[i]).intValue();
                } else {
                    frame[frameIndex++] = Frame.UNINITIALIZED
                            | cw.addUninitializedType("",
                                    ((Label) local[i]).position);
                }
            }
            for (int i = 0; i < nStack; ++i) {
                if (stack[i] instanceof String) {
                    frame[frameIndex++] = Frame.OBJECT
                            | cw.addType((String) stack[i]);
                } else if (stack[i] instanceof Integer) {
                    frame[frameIndex++] = ((Integer) stack[i]).intValue();
                } else {
                    frame[frameIndex++] = Frame.UNINITIALIZED
                            | cw.addUninitializedType("",
                                    ((Label) stack[i]).position);
                }
            }
            endFrame();
        } else {
            int delta;
            if (stackMap == null) {
                stackMap = new ByteVector();
                delta = code.length;
            } else {
                delta = code.length - previousFrameOffset - 1;
                if (delta < 0) {
                    if (type == Opcodes.F_SAME) {
                        return;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }

            switch (type) {
            case Opcodes.F_FULL:
                currentLocals = nLocal;
                stackMap.putByte(FULL_FRAME).putShort(delta).putShort(nLocal);
                for (int i = 0; i < nLocal; ++i) {
                    writeFrameType(local[i]);
                }
                stackMap.putShort(nStack);
                for (int i = 0; i < nStack; ++i) {
                    writeFrameType(stack[i]);
                }
                break;
            case Opcodes.F_APPEND:
                currentLocals += nLocal;
                stackMap.putByte(SAME_FRAME_EXTENDED + nLocal).putShort(delta);
                for (int i = 0; i < nLocal; ++i) {
                    writeFrameType(local[i]);
                }
                break;
            case Opcodes.F_CHOP:
                currentLocals -= nLocal;
                stackMap.putByte(SAME_FRAME_EXTENDED - nLocal).putShort(delta);
                break;
            case Opcodes.F_SAME:
                if (delta < 64) {
                    stackMap.putByte(delta);
                } else {
                    stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
                }
                break;
            case Opcodes.F_SAME1:
                if (delta < 64) {
                    stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
                } else {
                    stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                            .putShort(delta);
                }
                writeFrameType(stack[0]);
                break;
            }

            previousFrameOffset = code.length;
            ++frameCount;
        }

        maxStack = Math.max(maxStack, nStack);
        maxLocals = Math.max(maxLocals, currentLocals);
    }

    @Override
    public void visitInsn(final int opcode) {
        // adds the instruction to the bytecode of the method
        code.putByte(opcode);
        // update currentBlock
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            // updates current and max stack sizes
            int size = stackSize + Frame.SIZE[opcode];
            if (size > maxStackSize) {
                maxStackSize = size;
            }
            stackSize = size;
            // if opcode == ATHROW or xRETURN, ends current block (no successor)
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                    || opcode == Opcodes.ATHROW) {
                noSuccessor();
            }
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (opcode != Opcodes.NEWARRAY) {
                // updates current and max stack sizes only for NEWARRAY
                // (stack size variation = 0 for BIPUSH or SIPUSH)
                int size = stackSize + 1;
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        if (opcode == Opcodes.SIPUSH) {
            code.put12(opcode, operand);
        } else { // BIPUSH or NEWARRAY
            code.put11(opcode, operand);
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            // updates current and max stack sizes
            if (opcode == Opcodes.RET) {
                // no stack change, but end of current block (no successor)
                currentBlock.status |= Label.RET;
                // save 'stackSize' here for future use
                // (see {@link #findSubroutineSuccessors})
                currentBlock.inputStackTop = stackSize;
                noSuccessor();
            } else { // xLOAD or xSTORE
                int size = stackSize + Frame.SIZE[opcode];
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        
        // updates max locals
        int n;
        if (opcode == Opcodes.LLOAD || opcode == Opcodes.DLOAD
                || opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE) {
            n = var + 2;
        } else {
            n = var + 1;
        }
        if (n > maxLocals) {
            maxLocals = n;
        }
        // adds the instruction to the bytecode of the method
        if (var < 4 && opcode != Opcodes.RET) {
            int opt;
            if (opcode < Opcodes.ISTORE) {
                /* ILOAD_0 */
                opt = 26 + ((opcode - Opcodes.ILOAD) << 2) + var;
            } else {
                /* ISTORE_0 */
                opt = 59 + ((opcode - Opcodes.ISTORE) << 2) + var;
            }
            code.putByte(opt);
        } else if (var >= 256) {
            code.putByte(196 /* WIDE */).put12(opcode, var);
        } else {
            code.put11(opcode, var);
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        Item i = cw.newClassItem(type);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (opcode == Opcodes.NEW) {
                // updates current and max stack sizes only if opcode == NEW
                // (no stack change for ANEWARRAY, CHECKCAST, INSTANCEOF)
                int size = stackSize + 1;
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(opcode, i.index);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        Item i = cw.newFieldItem(owner, name, desc);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            int size;
            // computes the stack size variation
            char c = desc.charAt(0);
            switch (opcode) {
            case Opcodes.GETSTATIC:
                size = stackSize + (c == 'D' || c == 'J' ? 2 : 1);
                break;
            case Opcodes.PUTSTATIC:
                size = stackSize + (c == 'D' || c == 'J' ? -2 : -1);
                break;
            case Opcodes.GETFIELD:
                size = stackSize + (c == 'D' || c == 'J' ? 1 : 0);
                break;
            // case Constants.PUTFIELD:
            default:
                size = stackSize + (c == 'D' || c == 'J' ? -3 : -2);
                break;
            }
            // updates current and max stack sizes
            if (size > maxStackSize) {
                maxStackSize = size;
            }
            stackSize = size;
        }
        // adds the instruction to the bytecode of the method
        code.put12(opcode, i.index);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        Item i = cw.newMethodItem(owner, name, desc, itf);
        int argSize = i.intVal;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            /*
             * computes the stack size variation. In order not to recompute
             * several times this variation for the same Item, we use the
             * intVal field of this item to store this variation, once it
             * has been computed. More precisely this intVal field stores
             * the sizes of the arguments and of the return value
             * corresponding to desc.
             */
            if (argSize == 0) {
                // the above sizes have not been computed yet,
                // so we compute them...
                argSize = Type.getArgumentsAndReturnSizes(desc);
                // ... and we save them in order
                // not to recompute them in the future
                i.intVal = argSize;
            }
            int size;
            if (opcode == Opcodes.INVOKESTATIC) {
                size = stackSize - (argSize >> 2) + (argSize & 0x03) + 1;
            } else {
                size = stackSize - (argSize >> 2) + (argSize & 0x03);
            }
            // updates current and max stack sizes
            if (size > maxStackSize) {
                maxStackSize = size;
            }
            stackSize = size;
        }
        // adds the instruction to the bytecode of the method
        if (opcode == Opcodes.INVOKEINTERFACE) {
            if (argSize == 0) {
                argSize = Type.getArgumentsAndReturnSizes(desc);
                i.intVal = argSize;
            }
            code.put12(Opcodes.INVOKEINTERFACE, i.index).put11(argSize >> 2, 0);
        } else {
            code.put12(opcode, i.index);
        }
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            // updates current stack size (max stack size unchanged
            // because stack size variation always negative in this
            // case)
            stackSize += Frame.SIZE[opcode];
            addSuccessor(stackSize, label);
        }
        // adds the instruction to the bytecode of the method
        if ((label.status & Label.RESOLVED) != 0
                && label.position - code.length < Short.MIN_VALUE) {
            /*
             * case of a backward jump with an offset < -32768. In this case we
             * automatically replace GOTO with GOTO_W, JSR with JSR_W and IFxxx
             * <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is the
             * "opposite" opcode of IFxxx (i.e., IFNE for IFEQ) and where <l'>
             * designates the instruction just after the GOTO_W.
             */
            if (opcode == Opcodes.GOTO) {
                code.putByte(200); // GOTO_W
            } else {
                code.putByte(opcode <= 166 ? ((opcode + 1) ^ 1) - 1
                        : opcode ^ 1);
                code.putShort(8); // jump offset
                code.putByte(200); // GOTO_W
            }
            label.put(this, code, code.length - 1, true);
        } else {
            /*
             * case of a backward jump with an offset >= -32768, or of a forward
             * jump with, of course, an unknown offset. In these cases we store
             * the offset in 2 bytes (which will be increased in
             * resizeInstructions, if needed).
             */
            code.putByte(opcode);
            label.put(this, code, code.length - 1, false);
        }
        if (currentBlock != null) {
            if (opcode == Opcodes.GOTO) {
                noSuccessor();
            }
        }
    }

    @Override
    public void visitLabel(final Label label) {
        // resolves previous forward references to label, if any
        label.resolve(this, code.length, code.data);
        // updates currentBlock
        if ((label.status & Label.DEBUG) != 0) {
            return;
        }
        if (currentBlock != null) {
            // ends current block (with one new successor)
            currentBlock.outputStackMax = maxStackSize;
            addSuccessor(stackSize, label);
        }
        // begins a new current block
        currentBlock = label;
        // resets the relative current and max stack sizes
        stackSize = 0;
        maxStackSize = 0;
        // updates the basic block list
        if (previousBlock != null) {
            previousBlock.successor = label;
        }
        previousBlock = label;
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        Item i = cw.newConstItem(cst);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            int size;
            // computes the stack size variation
            if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
                size = stackSize + 2;
            } else {
                size = stackSize + 1;
            }
            // updates current and max stack sizes
            if (size > maxStackSize) {
                maxStackSize = size;
            }
            stackSize = size;
        }
        // adds the instruction to the bytecode of the method
        int index = i.index;
        if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
            code.put12(20 /* LDC2_W */, index);
        } else if (index >= 256) {
            code.put12(19 /* LDC_W */, index);
        } else {
            code.put11(Opcodes.LDC, index);
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        // updates max locals
        int n = var + 1;
        if (n > maxLocals) {
            maxLocals = n;
        }
        // adds the instruction to the bytecode of the method
        if ((var > 255) || (increment > 127) || (increment < -128)) {
            code.putByte(196 /* WIDE */).put12(Opcodes.IINC, var)
                    .putShort(increment);
        } else {
            code.putByte(Opcodes.IINC).put11(var, increment);
        }
    }
    
    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final Label dflt, final Label... labels) {
        // adds the instruction to the bytecode of the method
        int source = code.length;
        code.putByte(Opcodes.TABLESWITCH);
        code.putByteArray(null, 0, (4 - code.length % 4) % 4);
        dflt.put(this, code, source, true);
        code.putInt(min).putInt(max);
        for (int i = 0; i < labels.length; ++i) {
            labels[i].put(this, code, source, true);
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
            final Label[] labels) {
        // adds the instruction to the bytecode of the method
        int source = code.length;
        code.putByte(Opcodes.LOOKUPSWITCH);
        code.putByteArray(null, 0, (4 - code.length % 4) % 4);
        dflt.put(this, code, source, true);
        code.putInt(labels.length);
        for (int i = 0; i < labels.length; ++i) {
            code.putInt(keys[i]);
            labels[i].put(this, code, source, true);
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels);
    }

    private void visitSwitchInsn(final Label dflt, final Label[] labels) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            // updates current stack size (max stack size unchanged)
            --stackSize;
            // adds current block successors
            addSuccessor(stackSize, dflt);
            for (int i = 0; i < labels.length; ++i) {
                addSuccessor(stackSize, labels[i]);
            }
            // ends current block
            noSuccessor();
        }
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        Item i = cw.newClassItem(desc);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            // updates current stack size (max stack size unchanged because
            // stack size variation always negative or null)
            stackSize += 1 - dims;
        }
        // adds the instruction to the bytecode of the method
        code.put12(Opcodes.MULTIANEWARRAY, i.index).putByte(dims);
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
        if (signature != null) {
            if (localVarType == null) {
                localVarType = new ByteVector();
            }
            ++localVarTypeCount;
            localVarType.putShort(start.position)
                    .putShort(end.position - start.position)
                    .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(signature))
                    .putShort(index);
        }
        if (localVar == null) {
            localVar = new ByteVector();
        }
        ++localVarCount;
        localVar.putShort(start.position)
                .putShort(end.position - start.position)
                .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc))
                .putShort(index);
        // updates max locals
        char c = desc.charAt(0);
        int n = index + (c == 'J' || c == 'D' ? 2 : 1);
        if (n > maxLocals) {
            maxLocals = n;
        }
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        if (lineNumber == null) {
            lineNumber = new ByteVector();
        }
        ++lineNumberCount;
        lineNumber.putShort(start.position);
        lineNumber.putShort(line);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {

        /*
         * control flow analysis algorithm: while the block stack is not
         * empty, pop a block from this stack, update the max stack size,
         * compute the true (non relative) begin stack size of the
         * successors of this block, and push these successors onto the
         * stack (unless they have already been pushed onto the stack).
         * Note: by hypothesis, the {@link Label#inputStackTop} of the
         * blocks in the block stack are the true (non relative) beginning
         * stack sizes of these blocks.
         */
        int max = 0;
        Label stack = labels;
        while (stack != null) {
            // pops a block from the stack
            Label l = stack;
            stack = stack.next;
            // computes the true (non relative) max stack size of this block
            int start = l.inputStackTop;
            int blockMax = start + l.outputStackMax;
            // updates the global max stack size
            if (blockMax > max) {
                max = blockMax;
            }
            // analyzes the successors of the block
            Edge b = l.successors;
            if ((l.status & Label.JSR) != 0) {
                // ignores the first edge of JSR blocks (virtual successor)
                b = b.next;
            }
            while (b != null) {
                l = b.successor;
                // if this successor has not already been pushed...
                if ((l.status & Label.PUSHED) == 0) {
                    // computes its true beginning stack size...
                    l.inputStackTop = b.info == Edge.EXCEPTION ? 1 : start
                            + b.info;
                    // ...and pushes it onto the stack
                    l.status |= Label.PUSHED;
                    l.next = stack;
                    stack = l;
                }
                b = b.next;
            }
        }
        this.maxStack = Math.max(maxStack, max);
    }

    @Override
    public void visitEnd() {
    }

    // ------------------------------------------------------------------------
    // Utility methods: control flow analysis algorithm
    // ------------------------------------------------------------------------

    /**
     * Adds a successor to the {@link #currentBlock currentBlock} block.
     * 
     * @param info
     *            information about the control flow edge to be added.
     * @param successor
     *            the successor block to be added to the current block.
     */
    private void addSuccessor(final int info, final Label successor) {
        // creates and initializes an Edge object...
        Edge b = new Edge();
        b.info = info;
        b.successor = successor;
        // ...and adds it to the successor list of the currentBlock block
        b.next = currentBlock.successors;
        currentBlock.successors = b;
    }

    /**
     * Ends the current basic block. This method must be used in the case where
     * the current basic block does not have any successor.
     */
    private void noSuccessor() {
        currentBlock.outputStackMax = maxStackSize;
        currentBlock = null;
    }

    // ------------------------------------------------------------------------
    // Utility methods: stack map frames
    // ------------------------------------------------------------------------

    /**
     * Visit the implicit first frame of this method.
     */
    private void visitImplicitFirstFrame() {
        // There can be at most descriptor.length() + 1 locals
        int frameIndex = startFrame(0, descriptor.length() + 1, 0);
        if ((access & Opcodes.ACC_STATIC) == 0) {
            if ((access & ACC_CONSTRUCTOR) == 0) {
                frame[frameIndex++] = Frame.OBJECT | cw.addType(cw.thisName);
            } else {
                frame[frameIndex++] = 6; // Opcodes.UNINITIALIZED_THIS;
            }
        }
        int i = 1;
        loop: while (true) {
            int j = i;
            switch (descriptor.charAt(i++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                frame[frameIndex++] = 1; // Opcodes.INTEGER;
                break;
            case 'F':
                frame[frameIndex++] = 2; // Opcodes.FLOAT;
                break;
            case 'J':
                frame[frameIndex++] = 4; // Opcodes.LONG;
                break;
            case 'D':
                frame[frameIndex++] = 3; // Opcodes.DOUBLE;
                break;
            case '[':
                while (descriptor.charAt(i) == '[') {
                    ++i;
                }
                if (descriptor.charAt(i) == 'L') {
                    ++i;
                    while (descriptor.charAt(i) != ';') {
                        ++i;
                    }
                }
                frame[frameIndex++] = Frame.OBJECT
                        | cw.addType(descriptor.substring(j, ++i));
                break;
            case 'L':
                while (descriptor.charAt(i) != ';') {
                    ++i;
                }
                frame[frameIndex++] = Frame.OBJECT
                        | cw.addType(descriptor.substring(j + 1, i++));
                break;
            default:
                break loop;
            }
        }
        frame[1] = frameIndex - 3;
        endFrame();
    }

    /**
     * Starts the visit of a stack map frame.
     * 
     * @param offset
     *            the offset of the instruction to which the frame corresponds.
     * @param nLocal
     *            the number of local variables in the frame.
     * @param nStack
     *            the number of stack elements in the frame.
     * @return the index of the next element to be written in this frame.
     */
    private int startFrame(final int offset, final int nLocal, final int nStack) {
        int n = 3 + nLocal + nStack;
        if (frame == null || frame.length < n) {
            frame = new int[n];
        }
        frame[0] = offset;
        frame[1] = nLocal;
        frame[2] = nStack;
        return 3;
    }

    /**
     * Checks if the visit of the current frame {@link #frame} is finished, and
     * if yes, write it in the StackMapTable attribute.
     */
    private void endFrame() {
        if (previousFrame != null) { // do not write the first frame
            if (stackMap == null) {
                stackMap = new ByteVector();
            }
            writeFrame();
            ++frameCount;
        }
        previousFrame = frame;
        frame = null;
    }

    /**
     * Compress and writes the current frame {@link #frame} in the StackMapTable
     * attribute.
     */
    private void writeFrame() {
        int clocalsSize = frame[1];
        int cstackSize = frame[2];
        if ((cw.version & 0xFFFF) < Opcodes.V1_6) {
            stackMap.putShort(frame[0]).putShort(clocalsSize);
            writeFrameTypes(3, 3 + clocalsSize);
            stackMap.putShort(cstackSize);
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize);
            return;
        }
        int localsSize = previousFrame[1];
        int type = FULL_FRAME;
        int k = 0;
        int delta;
        if (frameCount == 0) {
            delta = frame[0];
        } else {
            delta = frame[0] - previousFrame[0] - 1;
        }
        if (cstackSize == 0) {
            k = clocalsSize - localsSize;
            switch (k) {
            case -3:
            case -2:
            case -1:
                type = CHOP_FRAME;
                localsSize = clocalsSize;
                break;
            case 0:
                type = delta < 64 ? SAME_FRAME : SAME_FRAME_EXTENDED;
                break;
            case 1:
            case 2:
            case 3:
                type = APPEND_FRAME;
                break;
            }
        } else if (clocalsSize == localsSize && cstackSize == 1) {
            type = delta < 63 ? SAME_LOCALS_1_STACK_ITEM_FRAME
                    : SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
        }
        if (type != FULL_FRAME) {
            // verify if locals are the same
            int l = 3;
            for (int j = 0; j < localsSize; j++) {
                if (frame[l] != previousFrame[l]) {
                    type = FULL_FRAME;
                    break;
                }
                l++;
            }
        }
        switch (type) {
        case SAME_FRAME:
            stackMap.putByte(delta);
            break;
        case SAME_LOCALS_1_STACK_ITEM_FRAME:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
            writeFrameTypes(3 + clocalsSize, 4 + clocalsSize);
            break;
        case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(
                    delta);
            writeFrameTypes(3 + clocalsSize, 4 + clocalsSize);
            break;
        case SAME_FRAME_EXTENDED:
            stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
            break;
        case CHOP_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            break;
        case APPEND_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            writeFrameTypes(3 + localsSize, 3 + clocalsSize);
            break;
        // case FULL_FRAME:
        default:
            stackMap.putByte(FULL_FRAME).putShort(delta).putShort(clocalsSize);
            writeFrameTypes(3, 3 + clocalsSize);
            stackMap.putShort(cstackSize);
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize);
        }
    }

    /**
     * Writes some types of the current frame {@link #frame} into the
     * StackMapTableAttribute. This method converts types from the format used
     * in {@link Label} to the format used in StackMapTable attributes. In
     * particular, it converts type table indexes to constant pool indexes.
     * 
     * @param start
     *            index of the first type in {@link #frame} to write.
     * @param end
     *            index of last type in {@link #frame} to write (exclusive).
     */
    private void writeFrameTypes(final int start, final int end) {
        for (int i = start; i < end; ++i) {
            int t = frame[i];
            int d = t & Frame.DIM;
            if (d == 0) {
                int v = t & Frame.BASE_VALUE;
                switch (t & Frame.BASE_KIND) {
                case Frame.OBJECT:
                    stackMap.putByte(7).putShort(
                            cw.newClass(cw.typeTable[v].strVal1));
                    break;
                case Frame.UNINITIALIZED:
                    stackMap.putByte(8).putShort(cw.typeTable[v].intVal);
                    break;
                default:
                    stackMap.putByte(v);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                d >>= 28;
                while (d-- > 0) {
                    sb.append('[');
                }
                if ((t & Frame.BASE_KIND) == Frame.OBJECT) {
                    sb.append('L');
                    sb.append(cw.typeTable[t & Frame.BASE_VALUE].strVal1);
                    sb.append(';');
                } else {
                    switch (t & 0xF) {
                    case 1:
                        sb.append('I');
                        break;
                    case 2:
                        sb.append('F');
                        break;
                    case 3:
                        sb.append('D');
                        break;
                    case 9:
                        sb.append('Z');
                        break;
                    case 10:
                        sb.append('B');
                        break;
                    case 11:
                        sb.append('C');
                        break;
                    case 12:
                        sb.append('S');
                        break;
                    default:
                        sb.append('J');
                    }
                }
                stackMap.putByte(7).putShort(cw.newClass(sb.toString()));
            }
        }
    }

    private void writeFrameType(final Object type) {
        if (type instanceof String) {
            stackMap.putByte(7).putShort(cw.newClass((String) type));
        } else if (type instanceof Integer) {
            stackMap.putByte(((Integer) type).intValue());
        } else {
            stackMap.putByte(8).putShort(((Label) type).position);
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: dump bytecode array
    // ------------------------------------------------------------------------

    /**
     * Returns the size of the bytecode of this method.
     * 
     * @return the size of the bytecode of this method.
     */
    final int getSize() {
        int size = 8;
        if (code.length > 0) {
            if (code.length > 65536) {
                throw new RuntimeException("Method code too large!");
            }
            cw.newUTF8("Code");
            size += 18 + code.length + 8 * handlerCount;
            if (localVar != null) {
                cw.newUTF8("LocalVariableTable");
                size += 8 + localVar.length;
            }
            if (localVarType != null) {
                cw.newUTF8("LocalVariableTypeTable");
                size += 8 + localVarType.length;
            }
            if (lineNumber != null) {
                cw.newUTF8("LineNumberTable");
                size += 8 + lineNumber.length;
            }
            if (stackMap != null) {
                boolean zip = (cw.version & 0xFFFF) >= Opcodes.V1_6;
                cw.newUTF8(zip ? "StackMapTable" : "StackMap");
                size += 8 + stackMap.length;
            }
        }
        if (exceptionCount > 0) {
            cw.newUTF8("Exceptions");
            size += 8 + 2 * exceptionCount;
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                cw.newUTF8("Synthetic");
                size += 6;
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            cw.newUTF8("Deprecated");
            size += 6;
        }
        if (methodParameters != null) {
            cw.newUTF8("MethodParameters");
            size += 7 + methodParameters.length;
        }
        return size;
    }

    /**
     * Puts the bytecode of this method in the given byte vector.
     * 
     * @param out
     *            the byte vector into which the bytecode of this method must be
     *            copied.
     */
    final void put(final ByteVector out) {
        final int FACTOR = ClassWriter.TO_ACC_SYNTHETIC;
        int mask = ACC_CONSTRUCTOR | Opcodes.ACC_DEPRECATED
                | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE
                | ((access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) / FACTOR);
        out.putShort(access & ~mask).putShort(name).putShort(desc);
        int attributeCount = 0;
        if (code.length > 0) {
            ++attributeCount;
        }
        if (exceptionCount > 0) {
            ++attributeCount;
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                ++attributeCount;
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            ++attributeCount;
        }
        if (methodParameters != null) {
            ++attributeCount;
        }
        out.putShort(attributeCount);
        if (code.length > 0) {
            int size = 12 + code.length + 8 * handlerCount;
            if (localVar != null) {
                size += 8 + localVar.length;
            }
            if (localVarType != null) {
                size += 8 + localVarType.length;
            }
            if (lineNumber != null) {
                size += 8 + lineNumber.length;
            }
            if (stackMap != null) {
                size += 8 + stackMap.length;
            }
            out.putShort(cw.newUTF8("Code")).putInt(size);
            out.putShort(maxStack).putShort(maxLocals);
            out.putInt(code.length).putByteArray(code.data, 0, code.length);
            out.putShort(handlerCount);
            attributeCount = 0;
            if (localVar != null) {
                ++attributeCount;
            }
            if (localVarType != null) {
                ++attributeCount;
            }
            if (lineNumber != null) {
                ++attributeCount;
            }
            if (stackMap != null) {
                ++attributeCount;
            }
            out.putShort(attributeCount);
            if (localVar != null) {
                out.putShort(cw.newUTF8("LocalVariableTable"));
                out.putInt(localVar.length + 2).putShort(localVarCount);
                out.putByteArray(localVar.data, 0, localVar.length);
            }
            if (localVarType != null) {
                out.putShort(cw.newUTF8("LocalVariableTypeTable"));
                out.putInt(localVarType.length + 2).putShort(localVarTypeCount);
                out.putByteArray(localVarType.data, 0, localVarType.length);
            }
            if (lineNumber != null) {
                out.putShort(cw.newUTF8("LineNumberTable"));
                out.putInt(lineNumber.length + 2).putShort(lineNumberCount);
                out.putByteArray(lineNumber.data, 0, lineNumber.length);
            }
            if (stackMap != null) {
                boolean zip = (cw.version & 0xFFFF) >= Opcodes.V1_6;
                out.putShort(cw.newUTF8(zip ? "StackMapTable" : "StackMap"));
                out.putInt(stackMap.length + 2).putShort(frameCount);
                out.putByteArray(stackMap.data, 0, stackMap.length);
            }
        }
        if (exceptionCount > 0) {
            out.putShort(cw.newUTF8("Exceptions")).putInt(
                    2 * exceptionCount + 2);
            out.putShort(exceptionCount);
            for (int i = 0; i < exceptionCount; ++i) {
                out.putShort(exceptions[i]);
            }
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                out.putShort(cw.newUTF8("Synthetic")).putInt(0);
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            out.putShort(cw.newUTF8("Deprecated")).putInt(0);
        }
        if (methodParameters != null) {
            out.putShort(cw.newUTF8("MethodParameters"));
            out.putInt(methodParameters.length + 1).putByte(
                    methodParametersCount);
            out.putByteArray(methodParameters.data, 0, methodParameters.length);
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: instruction resizing (used to handle GOTO_W and JSR_W)
    // ------------------------------------------------------------------------

    /**
     * Reads an unsigned short value in the given byte array.
     * 
     * @param b
     *            a byte array.
     * @param index
     *            the start index of the value to be read.
     * @return the read value.
     */
    static int readUnsignedShort(final byte[] b, final int index) {
        return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
    }

    /**
     * Reads a signed short value in the given byte array.
     * 
     * @param b
     *            a byte array.
     * @param index
     *            the start index of the value to be read.
     * @return the read value.
     */
    static short readShort(final byte[] b, final int index) {
        return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
    }

    /**
     * Reads a signed int value in the given byte array.
     * 
     * @param b
     *            a byte array.
     * @param index
     *            the start index of the value to be read.
     * @return the read value.
     */
    static int readInt(final byte[] b, final int index) {
        return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16)
                | ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
    }

    /**
     * Writes a short value in the given byte array.
     * 
     * @param b
     *            a byte array.
     * @param index
     *            where the first byte of the short value must be written.
     * @param s
     *            the value to be written in the given byte array.
     */
    static void writeShort(final byte[] b, final int index, final int s) {
        b[index] = (byte) (s >>> 8);
        b[index + 1] = (byte) s;
    }

    /**
     * Computes the future value of a bytecode offset.
     * <p>
     * Note: it is possible to have several entries for the same instruction in
     * the <tt>indexes</tt> and <tt>sizes</tt>: two entries (index=a,size=b) and
     * (index=a,size=b') are equivalent to a single entry (index=a,size=b+b').
     * 
     * @param indexes
     *            current positions of the instructions to be resized. Each
     *            instruction must be designated by the index of its <i>last</i>
     *            byte, plus one (or, in other words, by the index of the
     *            <i>first</i> byte of the <i>next</i> instruction).
     * @param sizes
     *            the number of bytes to be <i>added</i> to the above
     *            instructions. More precisely, for each i < <tt>len</tt>,
     *            <tt>sizes</tt>[i] bytes will be added at the end of the
     *            instruction designated by <tt>indexes</tt>[i] or, if
     *            <tt>sizes</tt>[i] is negative, the <i>last</i> |
     *            <tt>sizes[i]</tt>| bytes of the instruction will be removed
     *            (the instruction size <i>must not</i> become negative or
     *            null).
     * @param begin
     *            index of the first byte of the source instruction.
     * @param end
     *            index of the first byte of the target instruction.
     * @return the future value of the given bytecode offset.
     */
    static int getNewOffset(final int[] indexes, final int[] sizes,
            final int begin, final int end) {
        int offset = end - begin;
        for (int i = 0; i < indexes.length; ++i) {
            if (begin < indexes[i] && indexes[i] <= end) {
                // forward jump
                offset += sizes[i];
            } else if (end < indexes[i] && indexes[i] <= begin) {
                // backward jump
                offset -= sizes[i];
            }
        }
        return offset;
    }

    /**
     * Updates the offset of the given label.
     * 
     * @param indexes
     *            current positions of the instructions to be resized. Each
     *            instruction must be designated by the index of its <i>last</i>
     *            byte, plus one (or, in other words, by the index of the
     *            <i>first</i> byte of the <i>next</i> instruction).
     * @param sizes
     *            the number of bytes to be <i>added</i> to the above
     *            instructions. More precisely, for each i < <tt>len</tt>,
     *            <tt>sizes</tt>[i] bytes will be added at the end of the
     *            instruction designated by <tt>indexes</tt>[i] or, if
     *            <tt>sizes</tt>[i] is negative, the <i>last</i> |
     *            <tt>sizes[i]</tt>| bytes of the instruction will be removed
     *            (the instruction size <i>must not</i> become negative or
     *            null).
     * @param label
     *            the label whose offset must be updated.
     */
    static void getNewOffset(final int[] indexes, final int[] sizes,
            final Label label) {
        if ((label.status & Label.RESIZED) == 0) {
            label.position = getNewOffset(indexes, sizes, 0, label.position);
            label.status |= Label.RESIZED;
        }
    }
}
