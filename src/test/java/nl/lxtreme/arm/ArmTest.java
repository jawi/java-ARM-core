/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm;


import static org.junit.Assert.*;

import java.io.*;
import java.net.*;

import nl.lxtreme.arm.memory.*;
import nl.lxtreme.binutils.elf.*;
import nl.lxtreme.binutils.hex.*;

import org.junit.*;


/**
 * 
 */
public class ArmTest
{
  // CONSTANTS

  private static final int STACK_SIZE = (8 * 1024);

  private final int[] thumb = {
      0xb410, // push {r4}
      0xf64f, 0x7cee, // movw ip, #65518 ; 0xffee
      0xf243, 0x3444, // movw r4, #13124 ; 0x3344
      0x2300, // movs r3, #0
      0xf2c1, 0x1422, // movt r4, #4386 ; 0x1122
      0xf2c0, 0x0cc0, // movt ip, #192 ; 0xc0
      0x43d8, // mvns r0, r3
      0x6023, // str r3, [r4, #0]
      0x3301, // adds r3, #1
      0xf243, 0x3144, // movw r1, #13124 ; 0x3344
      0xf000, 0x00c3, // and.w r0, r0, #195 ; 0xc3
      0xf64f, 0x72ee, // movw r2, #65518 ; 0xffee
      0x2b80, // cmp r3, #128 ; 0x80
      0xf2c1, 0x1122, // movt r1, #4386 ; 0x1122
      0xf2c0, 0x02c0, // movt r2, #192 ; 0xc0
      0xf8cc, 0x0000, // str.w r0, [ip]
      0xd1ee, // bne.n 8014
      0x680b, // ldr r3, [r1, #0]
      0x6810, // ldr r0, [r2, #0]
      0x4218, // tst r0, r3
      0xd104, // bne.n 8048
      0x2399, // movs r3, #153 ; 0x99
      0x6013, // str r3, [r2, #0]
      0x2000, // movs r0, #0
      0xbc10, // pop {r4}
      0x4770, // bx lr
      0x2377, // movs r3, #119 ; 0x77
      0x600b, // str r3, [r1, #0]
      0xe7f9, // b.n 8042
      0xbf00 // nop
  };

  // VARIABLES

  private Memory m;
  private Arm arm;

  // METHODS

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception
  {
    this.m = new Memory();
    // Initial stack of 8Kb...
    this.m.create(0xFFFFFFFFL - STACK_SIZE, STACK_SIZE);
    this.arm = new Arm(this.m);
  }

  /**
   * 
   */
  @Test
  public void testB()
  {
    this.m.create(0, 4096);

    this.m.write32(0, 0xEA000006);
    this.arm.step();

    this.arm.reset();

    this.m.write32(0, 0xDA000009);
    this.arm.step();

    this.arm.reset();

    this.m.write32(0, 0x112fffff);
    this.arm.step();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testElfExample1() throws Exception
  {
    File elfFile = getFileResource("helloWorld_static");
    Elf elf = new Elf(elfFile);

    for (ProgramHeader ph : elf.getProgramHeaders())
    {
      int size = (int) ph.getMemorySize();
      if (size <= 0)
      {
        continue;
      }

      Chunk chunk = this.m.create(ph.getVirtualAddress(), size);
      elf.readSegment(ph, chunk);
    }

    this.arm = new Arm(this.m);
    this.arm.setPC((int) elf.getHeader().getEntryPoint());

    int i = 500;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testElfExample2() throws Exception
  {
    File elfFile = getFileResource("helloWorld_loop");
    Elf elf = new Elf(elfFile);

    for (ProgramHeader ph : elf.getProgramHeaders())
    {
      int size = (int) ph.getMemorySize();
      if (size <= 0)
      {
        continue;
      }

      Chunk chunk = this.m.create(ph.getVirtualAddress(), size);
      elf.readSegment(ph, chunk);
    }

    this.arm = new Arm(this.m);
    this.arm.setPC((int) elf.getHeader().getEntryPoint());

    int i = 500;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * 
   */
  @Test
  public void testExample1() throws Exception
  {
    this.m.create(0, 4096);

    loadIntelHexResource("blinkingLEDAndButton.hex");

    int i = 250;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * 
   */
  @Test
  public void testExample2() throws Exception
  {
    this.m.create(0, 4096);

    loadIntelHexResource("arm7tdmi_Blinky_iFlash.hex");

    int i = 250;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * 
   */
  @Test
  public void testExample3() throws Exception
  {
    this.m.create(0, 4096);

    loadIntelHexResource("led.hex");

    int i = 250;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * 
   */
  @Test
  public void testExample4() throws Exception
  {
    this.m.create(0, 4096);

    loadIntelHexResource("rtcAndInterruptExample.hex");

    int i = 250;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * 
   */
  @Test
  public void testExample5() throws Exception
  {
    this.m.create(0, 4096);

    loadIntelHexResource("blinking_leds.hex");

    int i = 250;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * 
   */
  @Test
  public void testThumbOk() throws Exception
  {
    this.m.create(0, 9216);

    for (int i = 0, address = 0; i < this.thumb.length; i++)
    {
      this.m.write16(address, (short) (this.thumb[i] & 0xFFFF));
      address += 2;
    }

    this.arm.forceThumbMode();

    int i = 250;
    while (i-- >= 0)
    {
      this.arm.step();
    }
  }

  /**
   * @param aName
   * @throws IOException
   */
  private File getFileResource(final String aName) throws IOException
  {
    URL url = getClass().getClassLoader().getResource(aName);
    if ((url != null) && "file".equals(url.getProtocol()))
    {
      return new File(url.getPath()).getCanonicalFile();
    }
    fail("Resource " + aName + " not found!");
    return null; // to keep compiler happy...
  }

  /**
   * @param aName
   * @throws IOException
   */
  private void loadIntelHexResource(final String aName) throws IOException
  {
    InputStream is = getClass().getClassLoader().getResourceAsStream(aName);
    if (is != null)
    {
      IntelHexReader reader = new IntelHexReader(new InputStreamReader(is));

      int instr = -1;
      int address = 0x00;
      while ((instr = reader.readLongWord()) != -1)
      {
        this.m.write32(address, instr);
        address += 4;
      }
    }
  }
}
