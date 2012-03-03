/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm.memory;


import static org.junit.Assert.*;

import org.junit.*;


/**
 * Test cases for {@link Chunk}.
 */
public class ChunkTest
{
  // VARIABLES

  private Chunk chunk;

  // METHODS

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception
  {
    this.chunk = new Chunk(0x0, 32);
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Memory#write16(int, short)}.
   */
  @Test
  public void testReadWrite16()
  {
    short v1 = (short) 0xabcd;
    short v2 = (short) 0x1234;

    this.chunk.write16(0, v1);
    this.chunk.write16(2, v2);

    assertEquals(v1, this.chunk.read16(0));
    assertEquals(v2, this.chunk.read16(2));
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Memory#write32(int, int)}.
   */
  @Test
  public void testReadWrite32()
  {
    int v1 = 0x12345678;
    int v2 = 0x9abcdef0;

    this.chunk.write32(0, v1);
    this.chunk.write32(4, v2);

    assertEquals(v1, this.chunk.read32(0));
    assertEquals(v2, this.chunk.read32(4));
  }

  /**
   * Test method for {@link nl.lxtreme.arm.Memory#write8(int, byte)}.
   */
  @Test
  public void testReadWrite8()
  {
    byte v1 = (byte) 0xAA;
    byte v2 = (byte) 0x55;

    this.chunk.write8(0, v1);
    this.chunk.write8(1, v2);

    assertEquals(v2, this.chunk.read8(1));
    assertEquals(v1, this.chunk.read8(0));
  }
}
