/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm;


import java.util.*;

import nl.lxtreme.arm.memory.*;


/**
 * Provides a simplistic version of an ARM-core.
 */
public class Arm
{
  // INNER TYPES

  /**
   * Denotes the "Current Program Status Register" (CPSR).
   */
  static class Cpsr
  {
    // VARIABLES

    boolean n; // 31
    boolean z; // 30
    boolean c; // 29
    boolean v; // 28
    boolean q; // 27

    int it; // 26, 25 & 15..10

    boolean j; // 24

    // padding 23..20

    int ge; // 19..16

    boolean E; // 9
    boolean A; // 8
    boolean I; // 7
    boolean F; // 6
    boolean t; // 5

    // 5-bits mode

    int mode; // 4..0

    // METHODS

    /**
     * Returns the value of the CPSR as 32-bit value.
     * 
     * @return a 32-bit value representation of the CPSR.
     */
    int getValue()
    {
      long result = 0;
      result |= (this.n ? (1L << 31) : 0);
      result |= (this.z ? (1L << 30) : 0);
      result |= (this.c ? (1L << 29) : 0);
      result |= (this.v ? (1L << 28) : 0);
      result |= (this.q ? (1L << 27) : 0);
      result |= ((this.it & 0x03) << 25);
      result |= (this.j ? (1L << 24) : 0);
      result |= ((this.ge & 0x0F) << 16);
      result |= ((this.it & 0xFC) << 8);
      result |= (this.E ? (1L << 9) : 0);
      result |= (this.A ? (1L << 8) : 0);
      result |= (this.I ? (1L << 7) : 0);
      result |= (this.F ? (1L << 6) : 0);
      result |= (this.t ? (1L << 5) : 0);
      result |= (this.mode & 0x1F);
      return (int) (result & 0xFFFFFFFF);
    }

    /**
     * Sets this CPSR by means of a 32-bit value.
     * 
     * @param aValue
     *          the 32-bit representation of the CPSR to set.
     */
    void setValue(int aValue)
    {
      long value = aValue;
      this.n = (value & (1L << 31)) != 0;
      this.z = (value & (1L << 30)) != 0;
      this.c = (value & (1L << 29)) != 0;
      this.v = (value & (1L << 28)) != 0;
      this.q = (value & (1L << 27)) != 0;
      this.it = (int) (((value >> 8) & 0xFC) | ((value >> 25) & 0x03));
      this.j = (value & (1L << 24)) != 0;
      this.ge = (int) ((value >> 16) & 0x0F);
      this.E = (value & (1L << 9)) != 0;
      this.A = (value & (1L << 8)) != 0;
      this.I = (value & (1L << 7)) != 0;
      this.F = (value & (1L << 6)) != 0;
      this.t = (value & (1L << 5)) != 0;
      this.mode = (int) (value & 0x1f);
    }
  }

  // VARIABLES

  private final int[] r;
  private final Cpsr cpsr;
  private int spsr;

  private boolean finished;
  private int entryPoint; // initial PC value

  private final List<Integer> breakpoints;
  private final Memory memory;

  // CONSTRUCTORS

  /**
   * Creates a new Arm instance.
   * 
   * @param aMemory
   *          the memory to use in the processor.
   */
  public Arm(Memory aMemory)
  {
    this.r = new int[16];
    this.cpsr = new Cpsr();

    this.memory = aMemory;
    this.breakpoints = new ArrayList<Integer>(32);
    this.entryPoint = 0;
  }

  // METHODS

  /**
   * Arithmetic Shift Right (ASR) moves each bit of a bitstring (x) right by a
   * specified number of bits (y). Copies of the leftmost bit are shifted in at
   * the left end of the bitstring. Bits that are shifted off the right end of
   * the bitstring are discarded, except that the last such bit can be produced
   * as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int ASR(int x, int y)
  {
    return (x >> y);
  }

  /**
   * Logical Shift Left (LSL) moves each bit of a bitstring (x) left by a
   * specified number of bits (y). Zeros are shifted in at the right end of the
   * bitstring. Bits that are shifted off the left end of the bitstring are
   * discarded, except that the last such bit can be produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int LSL(int x, int y)
  {
    return (x << y);
  }

  /**
   * Logical Shift Right (LSR) moves each bit of a bitstring (x) right by a
   * specified number of bits (y). Zeros are shifted in at the left end of the
   * bitstring. Bits that are shifted off the right end of the bitstring are
   * discarded, except that the last such bit can be produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int LSR(int x, int y)
  {
    return (x >>> y);
  }

  /**
   * Rotate Right (ROR) moves each bit of a bitstring (x) right by a specified
   * number of bits (y). Each bit that is shifted off the right end of the
   * bitstring is re-introduced at the left end. The last bit shifted off the
   * right end of the bitstring can be produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int ROR(int x, int y)
  {
    return ((x >>> y) | (x << (32 - y)));
  }

  /**
   * Rotate Right with Extend (RRX) moves each bit of a bitstring (x) right by
   * one bit. The carry input (c) is shifted in at the left end of the
   * bitstring. The bit shifted off the right end of the bitstring can be
   * produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param c
   *          the carry bit.
   * @return the bitshifted bitstring.
   */
  static int RRX(int x, int c)
  {
    return ((x >>> 1) | ((c & 0x01) << 31));
  }

  /**
   * Add breakpoint
   * 
   * @param address
   */
  public void breakAdd(int address)
  {
    if (!breakFind(address))
    {
      this.breakpoints.add(address);
    }
  }

  /**
   * @param address
   */
  public void breakDel(int address)
  {
    Iterator<Integer> iter = this.breakpoints.iterator();
    while (iter.hasNext())
    {
      Integer addr = iter.next();
      if ((addr != null) && (addr.intValue() == address))
      {
        iter.remove();
        break;
      }
    }
  }

  /**
   * @param address
   * @return
   */
  public boolean breakFind(int address)
  {
    Iterator<Integer> iter = this.breakpoints.iterator();
    while (iter.hasNext())
    {
      Integer addr = iter.next();
      if ((addr != null) && (addr.intValue() == address))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   */
  public void dumpRegs()
  {
    System.out.println("REGISTERS DUMP:");
    System.out.println("===============");

    /* Print GPRs */
    for (int i = 0; i < 16; i += 2)
    {
      System.out.printf("r%-2d: 0x%08X\t\tr%-2d: 0x%08X\n", i, this.r[i], i + 1, this.r[i + 1]);
    }

    System.out.println();

    /* Print CPSR */
    System.out.printf("cpsr: 0x%x\n", this.cpsr.getValue());
    System.out.println(" (z: " + this.cpsr.z + ", n: " + this.cpsr.n + ", c: " + this.cpsr.c + ", v: " + this.cpsr.v
         + ", I: " + this.cpsr.I + ", F: " + this.cpsr.F + ", t: " + this.cpsr.t + ", mode: " + this.cpsr.mode + ")");

    /* Print SPSR */
    System.out.printf("spsr: 0x%x\n", this.spsr);
  }

  /**
   * @param count
   */
  public void dumpStack(int count)
  {
    System.out.println("STACK DUMP:");
    System.out.println("===========");

    /* Print stack */
    for (int i = 0; i < count; i++)
    {
      int addr = this.r[13] + (i << 2);
      int value;

      /* Read stack */
      value = this.memory.read32(addr);

      /* Print value */
      System.out.printf("[%02d] 0x%08X\n", i, value);
    }
  }

  /**
   * @param idx
   * @return
   */
  public int peekReg(int idx)
  {
    return this.r[idx];
  }

  /**
   * @param idx
   * @param val
   */
  public void pokeReg(int idx, int val)
  {
    this.r[idx] = val;
  }

  /**
   * Resets this CPU.
   */
  public void reset()
  {
    Arrays.fill(this.r, 0);
    this.r[15] = this.entryPoint;
    this.cpsr.setValue(this.spsr = 0);
    this.finished = false;
  }

  /**
   * @param val
   */
  public void setPC(int val)
  {
    this.r[15] = this.entryPoint = val;
  }

  /**
   * Steps through the instructions.
   * 
   * @return <code>true</code> if a new instruction is available,
   *         <code>false</code> otherwise.
   */
  public boolean step()
  {
    boolean ret;

    /* Check finish flag */
    if (this.finished)
    {
      System.out.printf("FINISHED! (return: %d)", this.r[0]);
      return false;
    }

    /* Remove thumb bit */
    int pc = this.r[15] & ~1;

    /* Check breakpoint */
    ret = breakFind(pc);
    if (ret)
    {
      System.out.printf("BREAKPOINT! (0x%x)\n", pc);
      return false;
    }

    /* Parse instruction */
    if (this.cpsr.t)
    {
      parseThumb();
    }
    else
    {
      parse();
    }

    return true;
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected int addition(int a, int b)
  {
    /* Add values */
    int result = a + b;

    /* Set flags */
    this.cpsr.c = carryFrom(a, b);
    this.cpsr.v = overflowFrom(a, b);
    this.cpsr.z = result == 0;
    this.cpsr.n = ((result >> 31) != 0);

    return result;
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected boolean borrowFrom(int a, int b)
  {
    return (a < b) ? true : false; // TODO suspect!
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected boolean carryFrom(int a, int b)
  {
    return ((a + b) < a) ? true : false; // TODO suspect!
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   * @return
   */
  protected boolean condCheck(int opcode)
  {
    int condCheck = (opcode >> 28) & 0x0f;
    return conditionCheck(ConditionCode.values()[condCheck]);
  }

  /**
   * 16-bit opcode.
   * 
   * @param opcode
   * @return
   */
  protected boolean condCheck(short opcode)
  {
    int condCheck = (opcode >> 8) & 0x0f;
    return conditionCheck(ConditionCode.values()[condCheck]);
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   * @return
   */
  protected void condPrint(int opcode)
  {
    int condCheck = (opcode >> 28) & 0x0f;
    System.out.print(ConditionCode.values()[condCheck]);
  }

  /**
   * 16-bit opcode.
   * 
   * @param opcode
   * @return
   */
  protected void condPrint(short opcode)
  {
    int condCheck = (opcode >> 8) & 0x0f;
    System.out.print(ConditionCode.values()[condCheck]);
  }

  /**
   * 
   */
  protected void forceThumbMode()
  {
    this.cpsr.t = true;
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected boolean overflowFrom(int a, int b)
  {
    int s = a + b;

    if (((a & (1 << 31)) == (b & (1 << 31))) &&
        ((s & (1 << 31)) != (a & (1 << 31))))
    {
      return true;
    }

    return false;
  }

  /**
   * Parses the next ARM (32-bit) instruction.
   */
  protected void parse()
  {
    System.out.printf("%08X [A] ", this.r[15]);

    /* Read opcode */
    int opcode = this.memory.read32(this.r[15]);

    System.out.printf("(%08x) ", opcode);

    /* Update PC */
    this.r[15] += 4; // 32-bit

    /* Registers */
    int Rn = ((opcode >> 16) & 0xF);
    int Rd = ((opcode >> 12) & 0xF);
    int Rm = ((opcode >> 0) & 0xF);
    int Rs = ((opcode >> 8) & 0xF);
    int Imm = ((opcode >> 0) & 0xFF);
    int amt = Rs << 1;

    /* Flags */
    boolean I = ((opcode >> 25) & 1) != 0;
    boolean P = ((opcode >> 24) & 1) != 0;
    boolean U = ((opcode >> 23) & 1) != 0;
    boolean B = ((opcode >> 22) & 1) != 0;
    boolean W = ((opcode >> 21) & 1) != 0;
    boolean S = ((opcode >> 20) & 1) != 0;
    boolean L = ((opcode >> 20) & 1) != 0;

    if (((opcode >> 8) & 0x0FFFFF) == 0x012FFF)
    {
      boolean link = ((opcode >> 5) & 1) != 0;

      System.out.printf("b%sx", (link) ? "l" : "");
      condPrint(opcode);
      System.out.printf(" r%d\n", Rm);

      if (!condCheck(opcode))
      {
        return;
      }

      if (link)
      {
        this.r[14] = this.r[15];
      }

      this.cpsr.t = (this.r[Rm] & 1) == 1;

      this.r[15] = this.r[Rm] & ~1;

      return;
    }

    if ((opcode >> 24) == 0xEF)
    {
      int ImmA = (opcode & 0xFFFFFF);

      System.out.printf("swi 0x%X\n", ImmA);
      parseSvc(ImmA & 0xFF);

      return;
    }

    if ((((opcode >> 22) & 0x3F) == 0) &&
        (((opcode >> 4) & 0x0F) == 9))
    {
      System.out.printf("%s", W ? "mla" : "mul");
      condPrint(opcode);
      suffPrint(opcode);

      System.out.printf(" r%d, r%d, r%d", Rn, Rm, Rs);
      if (W)
      {
        System.out.printf(", r%d", Rd);
      }
      System.out.printf("\n");

      if (!condCheck(opcode))
      {
        return;
      }

      if (W)
      {
        this.r[Rn] = (this.r[Rm] * this.r[Rs] + this.r[Rd]) & 0xFFFFFFFF;
      }
      else
      {
        this.r[Rn] = (this.r[Rm] * this.r[Rs]) & 0xFFFFFFFF;
      }

      if (S)
      {
        this.cpsr.z = this.r[Rn] == 0;
        this.cpsr.n = (this.r[Rn] >> 31) != 0;
      }

      return;
    }

    switch ((opcode >> 26) & 0x3)
    {
      case 0:
      {
        switch ((opcode >> 21) & 0xF)
        {
          case 0:
          { // AND
            System.out.printf("and");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] & ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = this.r[Rn] & shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 1:
          { // EOR
            System.out.printf("eor");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] ^ ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = this.r[Rn] ^ shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 2:
          { // SUB
            System.out.printf("sub");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] - ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = this.r[Rn] - shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.c = (I) ? (this.r[Rn] >= ROR(Imm, amt)) : (this.r[Rn] < this.r[Rd]);
              this.cpsr.v = (I) ? ((this.r[Rn] >> 31) & ~(this.r[Rd] >> 31)) != 0
                  : ((this.r[Rn] >> 31) & ~(this.r[Rd] >> 31)) != 0;
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 3:
          { // RSB
            System.out.printf("rsb");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = ROR(Imm, amt) - this.r[Rn];
            }
            else
            {
              this.r[Rd] = shift(opcode, this.r[Rm]) - this.r[Rn];
            }

            if (S)
            {
              this.cpsr.c = (I) ? (this.r[Rn] > Imm) : (this.r[Rn] > this.r[Rm]);
              this.cpsr.v = (I) ? ((Imm >> 31) & ~((Imm - this.r[Rn]) >> 31)) != 0
                  : ((Imm >> 31) & ~((this.r[Rm] - this.r[Rn]) >> 31)) != 0;
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 4:
          { // ADD
            System.out.printf("add");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] + ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = this.r[Rn] + shift(opcode, this.r[Rm]);
            }

            if (Rn == 15)
            {
              this.r[Rd] += 4;
            }

            if (S)
            {
              this.cpsr.c = this.r[Rd] < this.r[Rn];
              this.cpsr.v = ((this.r[Rn] >> 31) & ~(this.r[Rd] >> 31)) != 0;
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 5:
          { // ADC
            System.out.printf("adc");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] + ROR(Imm, amt) + (this.cpsr.c ? 1 : 0);
            }
            else
            {
              this.r[Rd] = this.r[Rn] + shift(opcode, this.r[Rm]) + (this.cpsr.c ? 1 : 0);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 6:
          { // SBC
            System.out.printf("sbc");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] - ROR(Imm, amt) - (this.cpsr.c ? 0 : 1);
            }
            else
            {
              this.r[Rd] = this.r[Rn] - shift(opcode, this.r[Rm]) - (this.cpsr.c ? 0 : 1);
            }

            if (S)
            {
              this.cpsr.c = this.r[Rd] > this.r[Rn];
              this.cpsr.v = ((this.r[Rn] >> 31) & ~(this.r[Rd] >> 31)) != 0;
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 7:
          { // RSC
            System.out.printf("rsc");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = ROR(Imm, amt) - this.r[Rn] - (this.cpsr.c ? 0 : 1);
            }
            else
            {
              this.r[Rd] = shift(opcode, this.r[Rm]) - this.r[Rn] - (this.cpsr.c ? 0 : 1);
            }

            if (S)
            {
              this.cpsr.c = (I) ? (this.r[Rd] > Imm) : (this.r[Rd] > this.r[Rm]);
              this.cpsr.v = (I) ? ((this.r[Rm] >> 31) & ~(this.r[Rd] >> 31)) != 0
                  : ((this.r[Rn] >> 31) & ~(this.r[Rd] >> 31)) != 0;
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 8:
          { // TST/MRS
            if (S)
            {
              int result;

              System.out.printf("tst");
              condPrint(opcode);

              if (!I)
              {
                System.out.printf(" r%d, r%d\n", Rn, Rm);
                shiftPrint(opcode);

                result = this.r[Rn] & shift(opcode, this.r[Rm]);
              }
              else
              {
                System.out.printf(" r%d, #0x%X\n", Rn, ROR(Imm, amt));
                result = this.r[Rn] & ROR(Imm, amt);
              }

              this.cpsr.z = result == 0;
              this.cpsr.n = (result >> 31) != 0;
            }
            else
            {
              System.out.printf("mrs r%d, cpsr\n", Rd);
              this.r[Rd] = this.cpsr.getValue();
            }

            return;
          }

          case 9:
          { // TEQ/MSR
            if (S)
            {
              int result;

              System.out.printf("teq");
              condPrint(opcode);

              if (!I)
              {
                System.out.printf(" r%d, r%d\n", Rn, Rm);
                shiftPrint(opcode);

                result = this.r[Rn] ^ shift(opcode, this.r[Rm]);
              }
              else
              {
                System.out.printf(" r%d, #0x%X\n", Rn, ROR(Imm, amt));
                result = this.r[Rn] ^ ROR(Imm, amt);
              }

              this.cpsr.z = result == 0;
              this.cpsr.n = (result >> 31) != 0;
            }
            else
            {
              if (I)
              {
                System.out.printf("msr cpsr, r%d\n", Rm);
                this.cpsr.setValue(this.r[Rm]);
              }
              else
              {
                System.out.printf("msr cpsr, 0x%08X\n", Imm);
                this.cpsr.setValue(Imm);
              }
            }

            return;
          }

          case 10:
          { // CMP/MRS2
            if (S)
            {
              int value;

              System.out.printf("cmp");
              condPrint(opcode);

              if (I)
              {
                value = ROR(Imm, amt);
                System.out.printf(" r%d, 0x%08X\n", Rn, value);
              }
              else
              {
                value = this.r[Rm];
                System.out.printf(" r%d, r%d\n", Rn, Rm);
              }

              if (condCheck(opcode))
              {
                subtract(this.r[Rn], value);
              }
            }
            else
            {
              System.out.printf("mrs2\n");
            }

            return;
          }

          case 11:
          { // CMN/MSR2
            if (S)
            {
              int value;

              System.out.printf("cmn");
              condPrint(opcode);

              if (I)
              {
                value = ROR(Imm, amt);
                System.out.printf(" r%d, 0x%08X\n", Rn, value);
              }
              else
              {
                value = this.r[Rm];
                System.out.printf(" r%d, r%d\n", Rn, Rm);
              }

              if (condCheck(opcode))
              {
                addition(this.r[Rn], value);
              }
            }
            else
            {
              System.out.printf("msr2\n");
            }

            return;
          }

          case 12:
          { // ORR
            System.out.printf("orr");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] | ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = this.r[Rn] | shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 13:
          { // MOV
            System.out.printf("mov");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d", Rd, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, #0x%X", Rd, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = (Rm == 15) ? (this.r[15] + 4 /* 32-bit */) : shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 14:
          { // BIC
            System.out.printf("bic");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = this.r[Rn] & ~(ROR(Imm, amt));
            }
            else
            {
              this.r[Rd] = this.r[Rd] & ~shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }

          case 15:
          { // MVN
            System.out.printf("mvn");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
              System.out.printf(" r%d, r%d", Rd, Rm);
              shiftPrint(opcode);
            }
            else
            {
              System.out.printf(" r%d, #0x%X", Rd, ROR(Imm, amt));
            }

            System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return;
            }

            if (I)
            {
              this.r[Rd] = ~ROR(Imm, amt);
            }
            else
            {
              this.r[Rd] = ~shift(opcode, this.r[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.r[Rd] == 0;
              this.cpsr.n = (this.r[Rd] >> 31) != 0;
            }

            return;
          }
        }
      }

      case 1:
      { // LDR/STR
        int addr, value = 0, wb;

        System.out.printf("%s%s", (L) ? "ldr" : "str", (B) ? "b" : "");
        condPrint(opcode);
        System.out.printf(" r%d,", Rd);

        Imm = opcode & 0xFFF;

        if (L && (Rn == 15))
        {
          addr = this.r[15] + Imm + 4;
          value = this.memory.read32(addr);

          if (condCheck(opcode))
          {
            this.r[Rd] = value;
          }

          System.out.printf(" =0x%X\n", value);
          return;
        }

        System.out.printf(" [r%d", Rn);

        if (I)
        {
          value = shift(opcode, this.r[Rm]);

          System.out.printf(", %sr%d", (U) ? "" : "-", Rm);
          shiftPrint(opcode);
        }
        else
        {
          value = Imm;
          System.out.printf(", #%s0x%X", (U) ? "" : "-", value);
        }
        System.out.printf("]%s\n", (W) ? "!" : "");

        if (!condCheck(opcode))
        {
          return;
        }

        if (U)
        {
          wb = this.r[Rn] + value;
        }
        else
        {
          wb = this.r[Rn] - value;
        }

        addr = (P) ? wb : this.r[Rn];

        if (L)
        {
          if (B)
          {
            this.r[Rd] = this.memory.read8(addr);
          }
          else
          {
            this.r[Rd] = this.memory.read32(addr);
          }
        }
        else
        {
          value = this.r[Rd];
          if (Rd == 15)
          {
            value += 8;
          }

          if (B)
          {
            this.memory.write8(addr, (byte) (value & 0xFF));
          }
          else
          {
            this.memory.write32(addr, value);
          }
        }

        if (W || !P)
        {
          this.r[Rn] = wb;
        }
        return;
      }

      default:
        break;
    }

    switch ((opcode >> 25) & 7)
    {
      case 4:
      { // LDM/STM
        int start = this.r[Rn];
        boolean pf = false;

        if (L)
        {
          System.out.printf("ldm");
          if (Rn == 13)
          {
            System.out.printf("%c%c", (P) ? 'e' : 'f', (U) ? 'd' : 'a');
          }
          else
          {
            System.out.printf("%c%c", (U) ? 'i' : 'd', (P) ? 'b' : 'a');
          }
        }
        else
        {
          System.out.printf("stm");
          if (Rn == 13)
          {
            System.out.printf("%c%c", (P) ? 'f' : 'e', (U) ? 'a' : 'd');
          }
          else
          {
            System.out.printf("%c%c", (U) ? 'i' : 'd', (P) ? 'b' : 'a');
          }
        }

        if (Rn == 13)
        {
          System.out.printf(" sp");
        }
        else
        {
          System.out.printf(" r%d", Rn);
        }

        if (W)
        {
          System.out.printf("!");
        }
        System.out.printf(", {");

        for (int i = 0; i < 16; i++)
        {
          if (((opcode >> i) & 1) != 0)
          {
            if (pf)
            {
              System.out.printf(", ");
            }
            System.out.printf("r%d", i);

            pf = true;
          }
        }

        System.out.printf("}");
        if (B)
        {
          System.out.printf("^");
          if ((opcode & (1 << 15)) != 0)
          {
            this.cpsr.setValue(this.spsr);
          }
        }
        System.out.printf("\n");

        if (L)
        {
          for (int i = 0; i < 16; i++)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (P)
              {
                start += U ? 4 : -4; // 32-bit
              }
              this.r[i] = this.memory.read32(start);
              if (!P)
              {
                start += U ? 4 : -4; // 32-bit
              }
            }
          }
        }
        else
        {
          for (int i = 15; i >= 0; i--)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (P)
              {
                start += U ? 4 : -4; // 32-bit
               }
              this.memory.write32(start, this.r[i]);
              if (!P)
              {
                start += U ? 4 : -4; // 32-bit
              }
            }
          }
        }

        if (W)
        {
          this.r[Rn] = start;
        }
        return;
      }

      case 5:
      { // B/BL
        boolean link = (opcode & (1 << 24)) != 0;

        System.out.printf("b%s", (link) ? "l" : "");
        condPrint(opcode);

        Imm = (opcode & 0xFFFFFF) << 2;
        if ((Imm & (1 << 25)) != 0)
        {
          Imm = ~(~Imm & 0xFFFFFF);
        }
        Imm += 4; // 32-bit

        System.out.printf(" 0x%08X\n", this.r[15] + Imm);

        if (!condCheck(opcode))
        {
          return;
        }

        if (link)
        {
          this.r[14] = this.r[15];
        }
        this.r[15] += Imm;

        return;
      }

      case 7:
      { // MRC
        System.out.printf("mrc ...\n");
        return;
      }
    }

    System.out.printf("Unknown opcode! (0x%08X)\n", opcode);

  }

  /**
   * 8-bit value
   * 
   * @param num
   */
  protected void parseSvc(int num)
  {
    /* Parse syscall */
    switch (num)
    {
      case 0:
      { // exit
        /* Set finish flag */
        this.finished = true;
        break;
      }

      case 4:
      { // write
        int fd = this.r[0];
        int addr = this.r[1];
        int len = this.r[2];

        /* No output descriptor */
        if ((fd < 1) || (fd > 2))
        {
          break;
        }

        /* Print string */
        for (int i = 0; i < len; i++)
        {
          System.out.printf("0x%x", this.memory.read8(addr + i));
        }

        /* Return value */
        this.r[0] = len;

        break;
      }

      default:
        System.out.printf("         [S] Unhandled syscall! (%02X)\n", num);
    }
  }

  /**
   * Parses a THUMB (16-bit) instruction.
   */
  protected void parseThumb()
  {
    System.out.printf("%08X [T] ", this.r[15]);

    /* Read opcode */
    int opcode = this.memory.read16(this.r[15]) & 0xFFFF;

    System.out.printf("(%04x) ", opcode);

    /* Update PC */
    this.r[15] += 2; // 16-bit

    if ((opcode >> 13) == 0)
    {
      int Imm = (opcode >> 6) & 0x1F;
      int Rn = (opcode >> 6) & 7;
      int Rm = (opcode >> 3) & 7;
      int Rd = (opcode >> 0) & 7;

      switch ((opcode >> 11) & 3)
      {
        case 0:
        { // LSL
          if ((Imm > 0) && (Imm <= 32))
          {
            this.cpsr.c = (this.r[Rd] & (1 << (32 - Imm))) != 0;
            this.r[Rd] = LSL(this.r[Rd], Imm);
          }

          if (Imm > 32)
          {
            this.cpsr.c = false;
            this.r[Rd] = 0;
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("lsl r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
          return;
        }

        case 1:
        { // LSR
          if ((Imm > 0) && (Imm <= 32))
          {
            this.cpsr.c = (this.r[Rd] & (1 << (Imm - 1))) != 0;
            this.r[Rd] = LSR(this.r[Rd], Imm);
          }

          if (Imm > 32)
          {
            this.cpsr.c = false;
            this.r[Rd] = 0;
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("lsr r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
          return;
        }

        case 2:
        { // ASR
          if ((Imm > 0) && (Imm <= 32))
          {
            this.cpsr.c = (this.r[Rd] & (1 << (Imm - 1))) != 0;
            this.r[Rd] = ASR(this.r[Rd], Imm);
          }

          if (Imm > 32)
          {
            this.cpsr.c = false;
            this.r[Rd] = 0;
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("asr r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
          return;
        }

        case 3:
        { // ADD, SUB
          if ((opcode & 0x400) != 0)
          {
            Imm &= 7;

            if ((opcode & 0x200) != 0)
            {
              this.r[Rd] = subtract(this.r[Rm], Imm);

              System.out.printf("sub r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
              return;
            }
            else
            {
              this.r[Rd] = addition(this.r[Rm], Imm);

              System.out.printf("add r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
            }
          }
          else
          {
            if ((opcode & 0x200) != 0)
            {
              this.r[Rd] = subtract(this.r[Rm], this.r[Rn]);

              System.out.printf("sub r%d, r%d, r%d\n", Rd, Rm, Rn);
              return;
            }
            else
            {
              this.r[Rd] = addition(this.r[Rm], this.r[Rn]);

              System.out.printf("add r%d, r%d, r%d\n", Rd, Rm, Rn);
              return;
            }
          }

          return;
        }
      }
    }

    if ((opcode >> 13) == 1)
    {
      int Imm = (opcode & 0xFF);
      int Rn = (opcode >> 8) & 7;

      switch ((opcode >> 11) & 3)
      {
        case 0:
        { // MOV
          this.r[Rn] = Imm;

          this.cpsr.z = this.r[Rn] == 0;
          this.cpsr.n = (this.r[Rn] >> 31) != 0;

          System.out.printf("mov r%d, #0x%02X\n", Rn, Imm);
          return;
        }

        case 1:
        { // CMP
          subtract(this.r[Rn], Imm);

          System.out.printf("cmp r%d, #0x%02X\n", Rn, Imm);
          return;
        }

        case 2:
        { // ADD
          this.r[Rn] = addition(this.r[Rn], Imm);

          System.out.printf("add r%d, #0x%02X\n", Rn, Imm);
          return;
        }

        case 3:
        { // SUB
          this.r[Rn] = subtract(this.r[Rn], Imm);

          System.out.printf("sub r%d, #0x%02X\n", Rn, Imm);
          return;
        }
      }
    }

    if ((opcode >> 10) == 0x10)
    {
      int Rd = opcode & 7;
      int Rm = (opcode >> 3) & 7;

      switch ((opcode >> 6) & 0xF)
      {
        case 0:
        { // AND
          this.r[Rd] &= this.r[Rm];

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("and r%d, r%d\n", Rd, Rm);
          return;
        }

        case 1:
        { // EOR
          this.r[Rd] ^= this.r[Rm];

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("eor r%d, r%d\n", Rd, Rm);
          return;
        }

        case 2:
        { // LSL
          int shift = this.r[Rm] & 0xFF;

          if ((shift > 0) && (shift <= 32))
          {
            this.cpsr.c = (this.r[Rd] & (1 << (32 - shift))) != 0;
            this.r[Rd] = LSL(this.r[Rd], shift);
          }

          if (shift > 32)
          {
            this.cpsr.c = false;
            this.r[Rd] = 0;
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("lsl r%d, r%d\n", Rd, Rm);
          return;
        }

        case 3:
        { // LSR
          int shift = this.r[Rm] & 0xFF;

          if ((shift > 0) && (shift <= 32))
          {
            this.cpsr.c = (this.r[Rd] & (1 << (shift - 1))) != 0;
            this.r[Rd] = LSR(this.r[Rd], shift);
          }

          if (shift > 32)
          {
            this.cpsr.c = false;
            this.r[Rd] = 0;
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("lsr r%d, r%d\n", Rd, Rm);
          return;
        }

        case 4:
        { // ASR
          int shift = this.r[Rm] & 0xFF;

          if ((shift > 0) && (shift < 32))
          {
            this.cpsr.c = (this.r[Rd] & (1 << (shift - 1))) != 0;
            this.r[Rd] = ASR(this.r[Rd], shift);
          }

          if (shift == 32)
          {
            this.cpsr.c = (this.r[Rd] >> 31) != 0;
            this.r[Rd] = 0;
          }

          if (shift > 32)
          {
            this.cpsr.c = false;
            this.r[Rd] = 0;
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("asr r%d, r%d\n", Rd, Rm);
          return;
        }

        case 5:
        { // ADC
          this.r[Rd] = addition(this.r[Rd], this.r[Rm]);
          this.r[Rd] = addition(this.r[Rd], this.cpsr.c ? 1 : 0);

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("adc r%d, r%d\n", Rd, Rm);
          return;
        }

        case 6:
        { // SBC
          this.r[Rd] = subtract(this.r[Rd], this.r[Rm]);
          this.r[Rd] = subtract(this.r[Rd], this.cpsr.c ? 0 : 1);

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("sbc r%d, r%d\n", Rd, Rm);
          return;
        }

        case 7:
        { // ROR
          int shift = this.r[Rm] & 0xFF;

          while (shift >= 32)
          {
            shift -= 32;
          }

          if (shift != 0)
          {
            this.cpsr.c = (this.r[Rd] & (1 << (shift - 1))) != 0;
            this.r[Rd] = ROR(this.r[Rd], shift);
          }

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("ror r%d, r%d\n", Rd, Rm);
          return;
        }

        case 8:
        { // TST
          int result = this.r[Rd] & this.r[Rm];

          this.cpsr.z = result == 0;
          this.cpsr.n = (result >> 31) != 0;

          System.out.printf("tst r%d, r%d\n", Rd, Rm);
          return;
        }

        case 9:
        { // NEG
          this.r[Rd] = -this.r[Rm];

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("neg r%d, r%d\n", Rd, Rm);
          return;
        }

        case 10:
        { // CMP
          subtract(this.r[Rd], this.r[Rm]);

          System.out.printf("cmp r%d, r%d\n", Rd, Rm);
          return;
        }

        case 11:
        { // CMN/MVN
          if ((opcode & 0x100) != 0)
          {
            this.r[Rd] = ~this.r[Rm];

            this.cpsr.z = this.r[Rd] == 0;
            this.cpsr.n = (this.r[Rd] >> 31) != 0;

            System.out.printf("mvn r%d, r%d\n", Rd, Rm);
          }
          else
          {
            addition(this.r[Rd], this.r[Rm]);

            System.out.printf("cmn r%d, r%d\n", Rd, Rm);
          }

          return;
        }

        case 12:
        { // ORR
          this.r[Rd] |= this.r[Rm];

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("orr r%d, r%d\n", Rd, Rm);
          return;
        }

        case 13:
        { // MUL
          this.r[Rd] *= this.r[Rm];

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("mul r%d, r%d\n", Rd, Rm);
          return;
        }

        case 14:
        { // BIC
          this.r[Rd] &= ~this.r[Rm];

          this.cpsr.z = this.r[Rd] == 0;
          this.cpsr.n = (this.r[Rd] >> 31) != 0;

          System.out.printf("bic r%d, r%d\n", Rd, Rm);
          return;
        }
      }
    }

    if ((opcode >> 7) == 0x8F)
    {
      int Rm = (opcode >> 3) & 0xF;

      this.r[14] = this.r[15] | 1;

      this.cpsr.t = (this.r[Rm] & 1) != 0;

      this.r[15] = this.r[Rm] & ~1;

      System.out.printf("blx r%d\n", Rm);
      return;
    }

    if ((opcode >> 10) == 0x11)
    {
      int Rd = ((opcode >> 4) & 8) | (opcode & 7);
      int Rm = ((opcode >> 3) & 0xF);

      switch ((opcode >> 8) & 3)
      {
        case 0:
        { // ADD
          this.r[Rd] = addition(this.r[Rd], this.r[Rm]);

          System.out.printf("add r%d, r%d\n", Rd, Rm);
          return;
        }

        case 1:
        { // CMP
          subtract(this.r[Rd], this.r[Rm]);

          System.out.printf("cmp r%d, r%d\n", Rd, Rm);
          return;
        }

        case 2:
        { // MOV (NOP)
          if ((Rd == 8) && (Rm == 8))
          {
            System.out.printf("nop\n");
            return;
          }

          this.r[Rd] = this.r[Rm];

          System.out.printf("mov r%d, r%d\n", Rd, Rm);
          return;
        }

        case 3:
        { // BX
          this.cpsr.t = (this.r[Rm] & 1) != 0;

          if (Rm == 15)
          {
            this.r[15] += 2; // 16-bit
          }
          else
          {
            this.r[15] = this.r[Rm] & ~1;
          }

          System.out.printf("bx r%d\n", Rm);
          return;
        }
      }
    }

    if ((opcode >> 11) == 9)
    {
      int Rd = (opcode >> 8) & 7;
      int Imm = (opcode & 0xFF);
      int addr = this.r[15] + (Imm << 2) + 2; // 16-bit

      this.r[Rd] = this.memory.read32(addr);

      System.out.printf("ldr r%d, =0x%08X\n", Rd, this.r[Rd]);
      return;
    }

    if ((opcode >> 12) == 5)
    {
      int Rd = (opcode >> 0) & 7;
      int Rn = (opcode >> 3) & 7;
      int Rm = (opcode >> 6) & 7;

      switch ((opcode >> 9) & 7)
      {
        case 0:
        { // STR
          int addr = this.r[Rn] + this.r[Rm];
          int value = this.r[Rd];

          this.memory.write32(addr, value);

          System.out.printf("str r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return;
        }

        case 2:
        { // STRB
          int addr = this.r[Rn] + this.r[Rm];
          byte value = (byte) (this.r[Rd] & 0xFF);

          this.memory.write8(addr, value);

          System.out.printf("strb r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return;
        }

        case 4:
        { // LDR
          int addr = this.r[Rn] + this.r[Rm];

          this.r[Rd] = this.memory.read32(addr);

          System.out.printf("ldr r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return;
        }

        case 6:
        { // LDRB
          int addr = this.r[Rn] + this.r[Rm];

          this.r[Rd] = this.memory.read8(addr);

          System.out.printf("ldrb r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return;
        }
      }
    }

    if ((opcode >> 13) == 3)
    {
      int Rd = (opcode >> 0) & 7;
      int Rn = (opcode >> 3) & 7;
      int Imm = (opcode >> 6) & 7;

      if ((opcode & 0x1000) != 0)
      {
        if ((opcode & 0x800) != 0)
        {
          int addr = this.r[Rn] + (Imm << 2);

          this.r[Rd] = this.memory.read8(addr);

          System.out.printf("ldrb r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm);
        }
        else
        {
          int addr = this.r[Rn] + (Imm << 2);
          byte value = (byte) (this.r[Rd] & 0xFF);

          this.memory.write8(addr, value);

          System.out.printf("strb r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm);
        }
      }
      else
      {
        if ((opcode & 0x800) != 0)
        {
          int addr = this.r[Rn] + (Imm << 2);

          this.r[Rd] = this.memory.read32(addr);

          System.out.printf("ldr r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 2);
        }
        else
        {
          int addr = this.r[Rn] + (Imm << 2);
          int value = this.r[Rd];

          this.memory.write32(addr, value);

          System.out.printf("str r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 2);
        }
      }

      return;
    }

    if ((opcode >> 12) == 8)
    {
      int Rd = (opcode >> 0) & 7;
      int Rn = (opcode >> 3) & 7;
      int Imm = (opcode >> 6) & 7;

      if ((opcode & 0x800) != 0)
      {
        int addr = this.r[Rn] + (Imm << 1);

        this.r[Rd] = this.memory.read16(addr);

        System.out.printf("ldrh r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 1);
      }
      else
      {
        int addr = this.r[Rn] + (Imm << 1);
        short value = (short) (this.r[Rd] & 0xFFFF);

        this.memory.write16(addr, value);

        System.out.printf("strh r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 1);
      }

      return;
    }

    if ((opcode >> 12) == 9)
    {
      int Rd = (opcode >> 8) & 7;
      int Imm = (opcode & 0xFF);

      if ((opcode & 0x800) != 0)
      {
        int addr = this.r[13] + (Imm << 2);

        this.r[Rd] = this.memory.read32(addr);

        System.out.printf("ldr r%d, [sp, 0x%02X]\n", Rd, Imm << 2);
      }
      else
      {
        int addr = this.r[13] + (Imm << 2);
        int value = this.r[Rd];

        this.memory.write32(addr, value);

        System.out.printf("str r%d, [sp, 0x%02X]\n", Rd, Imm << 2);
      }

      return;
    }

    if ((opcode >> 12) == 10)
    {
      int Rd = (opcode >> 8) & 7;
      int Imm = (opcode & 0xFF);

      if ((opcode & 0x800) != 0)
      {
        this.r[Rd] = this.r[13] + (Imm << 2);

        System.out.printf("add r%d, sp, #0x%02X\n", Rd, Imm << 2);
      }
      else
      {
        this.r[Rd] = (this.r[15] & ~2) + (Imm << 2);

        System.out.printf("add r%d, pc, #0x%02X\n", Rd, Imm << 2);
      }

      return;
    }

    if ((opcode >> 12) == 11)
    {
      switch ((opcode >> 9) & 7)
      {
        case 0:
        { // ADD/SUB
          int Imm = (opcode & 0x7F);

          if ((opcode & 0x80) != 0)
          {
            this.r[13] -= Imm << 2;
            System.out.printf("sub sp, #0x%02X\n", Imm << 2);
          }
          else
          {
            this.r[13] += Imm << 2;
            System.out.printf("add sp, #0x%02X\n", Imm << 2);
          }

          return;
        }

        case 2:
        { // PUSH
          boolean lrf = (opcode & 0x100) != 0;
          boolean pf = false;

          if (lrf)
          {
            push(this.r[14]);
          }

          for (int i = 7; i >= 0; i--)
          {
            if (((opcode >> i) & 1) != 0)
            {
              push(this.r[i]);
            }
          }

          System.out.printf("push {");

          for (int i = 0; i < 8; i++)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (pf)
              {
                System.out.printf(",");
              }
              System.out.printf("r%d", i);

              pf = true;
            }
          }

          if (lrf)
          {
            if (pf)
            {
              System.out.printf(",");
            }
            System.out.printf("lr");
          }

          System.out.printf("}\n");
          return;
        }

        case 6:
        { // POP
          boolean pcf = (opcode & 0x100) != 0;
          boolean pf = false;

          System.out.printf("pop {");

          for (int i = 0; i < 8; i++)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (pf)
              {
                System.out.printf(",");
              }
              System.out.printf("r%d", i);

              this.r[i] = pop();
              pf = true;
            }
          }

          if (pcf)
          {
            if (pf)
            {
              System.out.printf(",");
            }
            System.out.printf("pc");

            this.r[15] = pop();
            this.cpsr.t = (this.r[15] & 1) != 0;
          }

          System.out.printf("}\n");
          return;
        }
      }
    }

    if ((opcode >> 12) == 12)
    {
      int Rn = (opcode >> 8) & 7;

      if ((opcode & 0x800) != 0)
      {
        System.out.printf("ldmia r%d!, {", Rn);

        for (int i = 0; i < 8; i++)
        {
          if (((opcode >> i) & 1) != 0)
          {
            this.r[i] = this.memory.read32(this.r[Rn]);
            this.r[Rn] += 4;

            System.out.printf("r%d,", i);
          }
        }

        System.out.printf("}\n");
        return;
      }
      else
      {
        System.out.printf("stmia r%d!, {", Rn);

        for (int i = 0; i < 8; i++)
        {
          if (((opcode >> i) & 1) != 0)
          {
            this.memory.write32(this.r[Rn], this.r[i]);
            this.r[Rn] += 4;

            System.out.printf("r%d,", i);
          }
        }

        System.out.printf("}\n");
        return;
      }
    }

    if ((opcode >> 12) == 13)
    {
      int Imm = (opcode & 0xFF) << 1;

      if ((Imm & 0x100) != 0)
      {
        Imm = ~((~Imm) & 0xFF);
      }

      Imm += 2;

      System.out.printf("b");
      condPrint(opcode);
      System.out.printf(" 0x%08X\n", (this.r[15] + Imm));

      if (condCheck(opcode))
      {
        this.r[15] += Imm;
      }

      return;
    }

    if ((opcode >> 11) == 28)
    {
      int Imm = (opcode & 0x7FF) << 1;

      if ((Imm & (1 << 11)) != 0)
      {
        Imm = (~Imm) & 0xFFE;
        this.r[15] -= Imm;
      }
      else
      {
        this.r[15] += Imm + 2; // 16-bit
      }

      System.out.printf("b 0x%08X, 0x%X\n", this.r[15], Imm);
      return;
    }

    if ((opcode >> 11) == 0x1E)
    {
      boolean blx = false;
      if ((opcode & (1 << 11)) == 0)
      {
        // H = 0
        int Imm = ((opcode & 0x7FF) << 12);

        this.r[14] = this.r[15] + Imm;
      }
      else
      {
        // H = 1
        int temp = this.r[15];

        int Imm = ((opcode & 0x7FF) << 1);

        this.r[15] = this.r[14] + Imm;
        this.r[14] = temp | 1;

        if ((Imm & (1 << 22)) != 0)
        {
          Imm = (~Imm) & 0x7FFFFE;
          this.r[15] -= Imm;
        }
        else
        {
          this.r[15] += Imm + 2;
        }
      }

      if (blx)
      {
        this.cpsr.t = false;
        System.out.printf("blx 0x%08X\n", this.r[15]);
      }
      else
      {
        System.out.printf("bl 0x%08X\n", this.r[15]);
      }

      return;
    }

    System.out.printf("Unknown opcode! (0x%04X)\n", opcode);
  }

  /**
   * @return 32-bit value.
   */
  protected int pop()
  {
    int addr = this.r[13];

    this.r[13] += 4; // 32-bit

    /* Read value */
    return this.memory.read32(addr);
  }

  /**
   * 32-bit value.
   * 
   * @param value
   */
  protected void push(int value)
  {
    /* Update SP */
    this.r[13] -= 4; // 32-bit

    /* Write value */
    this.memory.write32(this.r[13], value);
  }

  /**
   * 32-bit value/opcode.
   * 
   * @param opcode
   * @param value
   * @return
   */
  protected int shift(int opcode, int value)
  {
    boolean signed = ((opcode >> 20) & 1) == 1;

    int amt = (opcode >> 7) & 0x1F;
    int result;

    if (amt == 0)
    {
      return value;
    }

    switch ((opcode >> 5) & 3)
    {
      case 0:
        if (signed)
        {
          this.cpsr.c = (value & (1 << (32 - amt))) == 1;
        }

        result = LSL(value, amt);
        break;
      case 1:
        if (signed)
        {
          this.cpsr.c = (value & (1 << (amt - 1))) == 1;
        }

        result = LSR(value, amt);
        break;
      case 2:
        if (signed)
        {
          this.cpsr.c = (value & (1 << (amt - 1))) == 1;
        }

        result = ASR(value, amt);
        break;
      case 3:
        result = ROR(value, amt);
        break;

      default:
        result = value;
    }

    return result;
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   */
  protected void shiftPrint(int opcode)
  {
    int amt = (opcode >> 7) & 0x1F;
    if (amt == 0)
    {
      return;
    }

    switch ((opcode >> 5) & 3)
    {
      case 0:
        System.out.printf(",LSL#%d", amt);
        break;
      case 1:
        System.out.printf(",LSR#%d", amt);
        break;
      case 2:
        System.out.printf(",ASR#%d", amt);
        break;
      case 3:
        System.out.printf(",ROR#%d", amt);
        break;
    }
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected int subtract(int a, int b)
  {
    /* Subtract values */
    int result = a - b;

    /* Set flags */
    this.cpsr.c = !borrowFrom(a, b);
    this.cpsr.v = overflowFrom(a, -b);
    this.cpsr.z = result == 0;
    this.cpsr.n = ((result >> 31) == 1);

    return result;
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   */
  protected void suffPrint(int opcode)
  {
    if (((opcode >> 20) & 1) != 0)
    {
      System.out.print("s");
    }
  }

  /**
   * @param aOpcode
   * @return
   */
  private boolean conditionCheck(ConditionCode aCode)
  {
    /* Check condition */
    switch (aCode)
    {
      case EQ:
        return this.cpsr.z;
      case NE:
        return !this.cpsr.z;
      case CS:
        return this.cpsr.c;
      case CC:
        return !this.cpsr.c;
      case MI:
        return this.cpsr.n;
      case PL:
        return !this.cpsr.n;
      case VS:
        return this.cpsr.v;
      case VC:
        return !this.cpsr.v;
      case HI:
        return (this.cpsr.c && !this.cpsr.z);
      case LS:
        return (!this.cpsr.c || this.cpsr.z);
      case GE:
        return (this.cpsr.n == this.cpsr.v);
      case LT:
        return (this.cpsr.n != this.cpsr.v);
      case GT:
        return ((this.cpsr.n == this.cpsr.v) && !this.cpsr.z);
      case LE:
        return ((this.cpsr.n != this.cpsr.v) || this.cpsr.z);
      case AL:
        return true;
    }

    return false;
  }
}
