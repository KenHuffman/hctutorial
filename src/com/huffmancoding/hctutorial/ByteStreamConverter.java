package com.huffmancoding.hctutorial;

/******************************************************************************

    HuffmanTutorial: The Huffman Coding sample code.
    Copyright (C) 2022 Kenneth D. Huffman.

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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * This reads/writes an unpacked files containing bytes.
 * It also knows how to read/write bytes that are part of the serialized
 * Huffman Tree.
 *
 * @author Ken Huffman
 */
public class ByteStreamConverter implements StreamConverter<Byte>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Comparator<Byte> getObjectComparator()
    {
        return Byte::compare;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consumeAllInput(InputStream is, Consumer<Byte> accumulator) throws IOException
    {
        int i; // 0 to 255
        while ((i = is.read()) >= 0)
        {
            byte b = (byte)(i > Byte.MAX_VALUE ? i - 256 : i); // -128 to 127
            accumulator.accept(b);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeHuffmanTreeObject(DataOutputStream os, Byte b) throws IOException
    {
        os.writeByte(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte readHuffmanTreeObject(DataInputStream is) throws IOException
    {
        return is.readByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAllToOutput(Iterator<Byte> iterator, OutputStream os)
        throws IOException
    {
        while (iterator.hasNext())
        {
            Byte b = iterator.next();
            os.write(b);
        }
    }
}
