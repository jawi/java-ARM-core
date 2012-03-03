/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm;


import static org.junit.Assert.*;
import nl.lxtreme.arm.Arm.Cpsr;

import org.junit.*;


/**
 * Test cases for {@link Cpsr}.
 */
public class CpsrTest
{
  // VARIABLES

  private Cpsr cpsr;

  // METHODS

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception
  {
    this.cpsr = new Cpsr();
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_A()
  {
    this.cpsr.A = true;
    assertEquals(1 << 8, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_c()
  {
    this.cpsr.c = true;
    assertEquals(1 << 29, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_E()
  {
    this.cpsr.E = true;
    assertEquals(1 << 9, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_F()
  {
    this.cpsr.F = true;
    assertEquals(1 << 6, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_ge()
  {
    this.cpsr.ge = 0x0F;
    assertEquals(0xF0000, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_I()
  {
    this.cpsr.I = true;
    assertEquals(1 << 7, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_it()
  {
    this.cpsr.it = 0xFF;
    assertEquals(0x600FC00, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_mode()
  {
    this.cpsr.mode = 0x1F;
    assertEquals(0x1F, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_n()
  {
    this.cpsr.n = true;
    assertEquals(1 << 31, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_q()
  {
    this.cpsr.q = true;
    assertEquals(1 << 27, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_t()
  {
    this.cpsr.t = true;
    assertEquals(1 << 5, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_v()
  {
    this.cpsr.v = true;
    assertEquals(1 << 28, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#getValue()}.
   */
  @Test
  public void testGetValue_z()
  {
    this.cpsr.z = true;
    assertEquals(1 << 30, this.cpsr.getValue());
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_A()
  {
    this.cpsr.setValue(1 << 8);
    assertTrue(this.cpsr.A);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_c()
  {
    this.cpsr.setValue(1 << 29);
    assertTrue(this.cpsr.c);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_E()
  {
    this.cpsr.setValue(1 << 9);
    assertTrue(this.cpsr.E);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_F()
  {
    this.cpsr.setValue(1 << 6);
    assertTrue(this.cpsr.F);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_ge()
  {
    this.cpsr.setValue(0xF0000);
    assertEquals(0x0F, this.cpsr.ge);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_I()
  {
    this.cpsr.setValue(1 << 7);
    assertTrue(this.cpsr.I);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_it()
  {
    this.cpsr.setValue(0x600FC00);
    assertEquals(0xFF, this.cpsr.it);

    this.cpsr.setValue(0xFC00);
    assertEquals(0xFC, this.cpsr.it);

    this.cpsr.setValue(0x6000000);
    assertEquals(0x3, this.cpsr.it);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_mode()
  {
    this.cpsr.setValue(0x1F);
    assertEquals(0x1F, this.cpsr.mode);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_n()
  {
    this.cpsr.setValue(1 << 31);
    assertTrue(this.cpsr.n);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_q()
  {
    this.cpsr.setValue(1 << 27);
    assertTrue(this.cpsr.q);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_t()
  {
    this.cpsr.setValue(1 << 5);
    assertTrue(this.cpsr.t);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_v()
  {
    this.cpsr.setValue(1 << 28);
    assertTrue(this.cpsr.v);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Arm.Cpsr#setValue(int)}.
   */
  @Test
  public void testSetValue_z()
  {
    this.cpsr.setValue(1 << 30);
    assertTrue(this.cpsr.z);
  }
}
