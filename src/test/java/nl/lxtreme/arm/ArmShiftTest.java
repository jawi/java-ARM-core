/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm;


import static nl.lxtreme.arm.Arm.*;
import static org.junit.Assert.*;

import org.junit.*;


/**
 * Tests the various shifting operations.
 */
public class ArmShiftTest
{
  // METHODS

  /**
   * Test method for {@link nl.lxtreme.arm.Arm#ASR(int, int)}.
   */
  @Test
  public void testASR()
  {
    assertEquals(0xC0000000, ASR(0x80000000, 1));
    assertEquals(0xE0000000, ASR(0x80000000, 2));
    assertEquals(0xF0000000, ASR(0x80000000, 3));
    assertEquals(0xF8000000, ASR(0x80000000, 4));
    assertEquals(0x04000000, ASR(0x08000000, 1));
    assertEquals(0x02000000, ASR(0x08000000, 2));
    assertEquals(0x01000000, ASR(0x08000000, 3));
    assertEquals(0x00000004, ASR(0x00000008, 1));
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm#LSL(int, int)}.
   */
  @Test
  public void testLSL()
  {
    assertEquals(0x80000000, LSL(0xC0000000, 1));
    assertEquals(0x80000000, LSL(0xE0000000, 2));
    assertEquals(0x80000000, LSL(0xF0000000, 3));
    assertEquals(0x80000000, LSL(0xF8000000, 4));
    assertEquals(0x08000000, LSL(0x04000000, 1));
    assertEquals(0x08000000, LSL(0x02000000, 2));
    assertEquals(0x08000000, LSL(0x01000000, 3));
    assertEquals(0x00000008, LSL(0x00000004, 1));
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm#LSR(int, int)}.
   */
  @Test
  public void testLSR()
  {
    assertEquals(0x40000000, LSR(0x80000000, 1));
    assertEquals(0x20000000, LSR(0x80000000, 2));
    assertEquals(0x10000000, LSR(0x80000000, 3));
    assertEquals(0x08000000, LSR(0x80000000, 4));
    assertEquals(0x04000000, LSR(0x08000000, 1));
    assertEquals(0x02000000, LSR(0x08000000, 2));
    assertEquals(0x01000000, LSR(0x08000000, 3));
    assertEquals(0x00000004, LSR(0x00000008, 1));
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm#ROR(int, int)}.
   */
  @Test
  public void testROR()
  {
    assertEquals(0x80000000, ROR(0x00000001, 1));
    assertEquals(0x40000000, ROR(0x00000001, 2));
    assertEquals(0x20000000, ROR(0x00000001, 3));
    assertEquals(0x20000000, ROR(0x80000000, 2));
    assertEquals(0x10000000, ROR(0x80000000, 3));
    assertEquals(0x08000000, ROR(0x80000000, 4));
    assertEquals(0x04000000, ROR(0x08000000, 1));
    assertEquals(0x02000000, ROR(0x08000000, 2));
    assertEquals(0x01000000, ROR(0x08000000, 3));
    assertEquals(0x00000004, ROR(0x00000008, 1));
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm#RRX(int, int)}.
   */
  @Test
  public void testRRX()
  {
    assertEquals(0x80000000, RRX(0x00000000, 1));
    assertEquals(0x80000000, RRX(0x00000001, 1));
    assertEquals(0x00000000, RRX(0x00000000, 0));
    assertEquals(0x00000000, RRX(0x00000001, 0));
  }

}
