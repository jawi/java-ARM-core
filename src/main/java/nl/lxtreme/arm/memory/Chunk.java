/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm.memory;


import java.io.*;
import java.util.*;


/**
 * Denotes a "chunk" of memory, located at a certain address and with a certain
 * size.
 */
public class Chunk extends OutputStream
{
  // VARIABLES

  private final long address;
  private final byte[] data;

  private long writePtr = 0L;

  // CONSTRUCTORS

  /**
   * Creates a new Chunk instance.
   * 
   * @param aAddress
   * @param aSize
   */
  public Chunk( long aAddress, int aSize )
  {
    if ( aSize <= 0 )
    {
      throw new IllegalArgumentException( "Illegal memory size!" );
    }

    this.address = aAddress;
    this.data = new byte[aSize];

    Arrays.fill( this.data, ( byte )0xff );
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    // NO-op
    this.writePtr = -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException
  {
    // NO-op
  }

  /**
   * Returns the base address for this chunk.
   * 
   * @return the base address, >= 0.
   */
  public long getBaseAddress()
  {
    return this.address;
  }

  /**
   * Returns the size of this chunk.
   * 
   * @return a chunk size, > 0.
   */
  public int getSize()
  {
    return this.data.length;
  }

  /**
   * Returns whether the given address is mapped by this chunk.
   * 
   * @param aAddress
   *          the address that should map to this chunk.
   * @return <code>true</code> if the given address maps to this chunk,
   *         <code>false</code> otherwise.
   */
  public boolean maps( long aAddress )
  {
    int addr = mapAddress( aAddress );
    return ( addr >= 0 ) && ( addr < getSize() );
  }

  /**
   * Reads a 16-bit value from this memory at the given address.
   * 
   * @param aAddr
   *          the memory location to read.
   * @return the 16-bit value at the given memory location.
   */
  public short read16( int aAddr )
  {
    int addr = mapAddress( aAddr );
    if ( validAddress( addr ) )
    {
      int msb = ( this.data[addr + 0] & 0xFF );
      int lsb = ( this.data[addr + 1] & 0xFF );
      return ( short )( ( msb << 8 ) | lsb );
    }
    else
    {
      throw new IllegalArgumentException( String.format( "Invalid address to read: 0x%08x", aAddr ) );
    }
  }

  /**
   * Reads a 32-bit value from this memory at the given address.
   * 
   * @param aAddr
   *          the memory location to read.
   * @return the 32-bit value at the given memory location.
   */
  public int read32( int aAddr )
  {
    int addr = mapAddress( aAddr );
    if ( validAddress( addr ) )
    {
      int b1 = this.data[addr + 0] & 0xff;
      int b2 = this.data[addr + 1] & 0xff;
      int b3 = this.data[addr + 2] & 0xff;
      int b4 = this.data[addr + 3] & 0xff;
      return ( b1 << 24 ) | ( b2 << 16 ) | ( b3 << 8 ) | b4;
    }
    else
    {
      throw new IllegalArgumentException( String.format( "Invalid address to read: 0x%08x", aAddr ) );
    }
  }

  /**
   * Reads a 8-bit value from this memory at the given address.
   * 
   * @param aAddr
   *          the memory location to read.
   * @return the 8-bit value at the given memory location.
   */
  public byte read8( int aAddr )
  {
    int addr = mapAddress( aAddr );
    if ( validAddress( addr ) )
    {
      return this.data[addr];
    }
    else
    {
      throw new IllegalArgumentException( String.format( "Invalid address to read: 0x%08x", aAddr ) );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return String.format( "Chunk @ 0x%08x: %d bytes", this.address, this.data.length );
  }

  @Override
  public void write( int aByte ) throws IOException
  {
    if ( this.writePtr < 0 )
    {
      throw new IOException( "Writer is closed!" );
    }

    this.data[( int )this.writePtr++] = ( byte )aByte;
  }

  /**
   * Writes a 16-bit value to the memory denoted by the given address.
   * 
   * @param aAddr
   *          the memory location to write;
   * @param aValue
   *          the 16-bit value to write.
   */
  public void write16( int aAddr, short aValue )
  {
    int addr = mapAddress( aAddr );
    if ( validAddress( addr ) )
    {
      this.data[addr + 0] = ( byte )( ( aValue >> 8 ) & 0xff );
      this.data[addr + 1] = ( byte )( aValue & 0xff );
    }
    else
    {
      throw new IllegalArgumentException( String.format( "Invalid address to write: 0x%08x", aAddr ) );
    }
  }

  /**
   * Writes a 32-bit value to the memory denoted by the given address.
   * 
   * @param aAddr
   *          the memory location to write;
   * @param aValue
   *          the 32-bit value to write.
   */
  public void write32( int aAddr, int aValue )
  {
    int addr = mapAddress( aAddr );
    if ( validAddress( addr ) )
    {
      this.data[addr + 0] = ( byte )( ( aValue >> 24 ) & 0xff );
      this.data[addr + 1] = ( byte )( ( aValue >> 16 ) & 0xff );
      this.data[addr + 2] = ( byte )( ( aValue >> 8 ) & 0xff );
      this.data[addr + 3] = ( byte )( aValue & 0xff );
    }
    else
    {
      throw new IllegalArgumentException( String.format( "Invalid address to write: 0x%08x", aAddr ) );
    }
  }

  /**
   * Writes a 8-bit value to the memory denoted by the given address.
   * 
   * @param aAddr
   *          the memory location to write;
   * @param aValue
   *          the 8-bit value to write.
   */
  public void write8( int aAddr, byte aValue )
  {
    int addr = mapAddress( aAddr );
    if ( validAddress( addr ) )
    {
      this.data[addr] = aValue;
    }
    else
    {
      throw new IllegalArgumentException( String.format( "Invalid address to write: 0x%08x", aAddr ) );
    }
  }

  /**
   * @param aAddr
   * @return
   */
  private int mapAddress( long aAddr )
  {
    long addr = aAddr - this.address;
    if ( addr < 0 )
    {
      addr -= 1;
    }
    return ( int )( addr & 0xFFFFFFFF );
  }

  /**
   * @param aAddr
   * @return
   */
  private boolean validAddress( int aAddr )
  {
    long addr = aAddr & 0xFFFFFFFF;
    return ( addr >= 0 ) && ( addr < this.data.length );
  }
}
