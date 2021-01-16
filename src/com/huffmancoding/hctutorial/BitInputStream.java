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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DataInputStream that allows individual bits to be read.
 * The underlying stream is read a byte at a time into the unreadByte
 * and the bits are returned from the highest bit to the lowest.
 *
 * @author Ken Huffman
 */
public class BitInputStream extends DataInputStream
{
    /** the highest order bit of a serialized byte. */
    private static final int HIGH_BIT_OF_BYTE = Byte.MAX_VALUE+1;

    /** the byte currently being parsed. */
    int unreadByte = 0;

    /** the position within the unread byte to read next.
        0 means the next byte should be read into unreadByte first. */
    int bitPosition = 0;

    /**
     * Constructor.
     *
     * @param in the wrapped InputStream
     */
    protected BitInputStream(InputStream in)
    {
        super(in);
    }

    /**
     * Read the next bit from the InputStream.
     *
     * @return the next bit
     * @throws IOException in case of read error.
     */
    public boolean readBit() throws IOException
    {
        if (bitPosition == 0)
        {
            unreadByte = super.readByte();
            bitPosition = HIGH_BIT_OF_BYTE;
        }

        boolean bit = ((unreadByte & bitPosition) != 0);
        bitPosition >>= 1;

        return bit;
    }
}
