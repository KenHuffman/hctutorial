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
import java.io.OutputStream;
import java.util.Iterator;

/**
 * This class uses Huffman Coding to unpack a file that packed byte-by-byte.
 *
 * @author Ken Huffman
 */
public class ByteFileUnpacker extends FileUnpacker<Byte>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Byte readObject(DataInputStream is) throws IOException
    {
        return is.readByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObjects(OutputStream os, Iterator<Byte> iterator)
        throws IOException
    {
        while (iterator.hasNext())
        {
            Byte b = iterator.next();
            os.write(b);
        }
    }
}
