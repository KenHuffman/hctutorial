package com.huffmancoding.hctutorial;

/******************************************************************************

    HuffmanTutorial: The Huffman Coding sample code.
    Copyright (C) 2002-2021 Kenneth D. Huffman.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License (www.gnu.org/copyleft/gpl.html) for more details.

    The author can be reached at huffmancoding.com.

    Although this is meant to explain Huffman coding, do not pass this code off
    as your own if given a homework assignment.

******************************************************************************/

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a DataOutputStream that allows individual bits to be written to
 * an OutputStream. Bits are accumulated from the highest to lowest bits of
 * a byte (128, 64, 32, 16, 8, 4, 2, 1). After 8 bits are written the byte
 * is flushed as a byte.
 *
 * @author Ken Huffman
 */
public class BitOutputStream extends DataOutputStream
{
    /** the highest order bit of a serialized byte. */
    private static final int HIGH_BIT_OF_BYTE = Byte.MAX_VALUE+1;

    /** the byte as it accumulates before being written. */
    int unwrittenByte = 0;

    /** the bit mask to be written to next. */
    int bitPosition = HIGH_BIT_OF_BYTE;

    /**
     * Constructor.
     *
     * @param out the wrapped OutputStream
     */
    public BitOutputStream(OutputStream out)
    {
        super(out);
    }

    /**
     * Write a single bit within the next byte.
     *
     * @param bit the bit to write
     * @throws IOException in case of write errors.
     */
    public void writeBit(boolean bit) throws IOException
    {
        if (bit)
        {
            unwrittenByte += bitPosition;
        }

        bitPosition >>= 1;
        if (bitPosition == 0)
        {
            flush();
        }
    }

    /**
     * Write the accumulated bits to the underlying stream
     */
    @Override
    public void flush() throws IOException
    {
        if (bitPosition != HIGH_BIT_OF_BYTE)
        {
            super.writeByte(unwrittenByte);
            unwrittenByte = 0;
            bitPosition = HIGH_BIT_OF_BYTE;
        }

        super.flush();
    }
}
