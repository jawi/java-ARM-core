/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm.memory;


import static org.junit.Assert.*;

import org.junit.*;


/**
 * Test cases for {@link Memory}.
 */
public class MemoryTest
{
  // VARIABLES

  private Memory memory;

  // METHODS

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception
  {
    this.memory = new Memory();
  }

  /**
   * Test method for {@link nl.lxtreme.arm.memory.Memory#create(long, int)}.
   */
  @Test
  public void testCreate()
  {
    Chunk c1 = this.memory.create(0, 100);
    assertNotNull(c1);

    Chunk c2 = this.memory.create(200, 100);
    assertNotSame(c1, c2);

    Chunk c3 = this.memory.create(300, 100);
    assertNotSame(c2, c3);

    Chunk c4 = this.memory.create(210, 100);
    assertSame(c4, c2);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.memory.Memory#find(long)}.
   */
  @Test
  public void testFind()
  {
    Chunk c1 = this.memory.create(0, 100);
    Chunk c2 = this.memory.create(200, 100);
    Chunk c3 = this.memory.create(300, 100);

    assertNull(this.memory.find(-1));
    assertSame(c1, this.memory.find(0));
    assertSame(c1, this.memory.find(50));
    assertSame(c1, this.memory.find(99));
    assertNull(this.memory.find(100));
    assertSame(c2, this.memory.find(200));
    assertSame(c2, this.memory.find(250));
    assertSame(c2, this.memory.find(299));
    assertSame(c3, this.memory.find(300));
    assertSame(c3, this.memory.find(350));
    assertSame(c3, this.memory.find(399));
    assertNull(this.memory.find(400));
  }
}
