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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

/**
 * This class uses Huffman Coding to unpack a file that originally had
 * characters.
 *
 * @author Ken Huffman
 */
public class CharacterFileUnpacker extends FileUnpacker<Character>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Character readObject(DataInputStream is) throws IOException
    {
        return is.readChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObjects(OutputStream os, Iterator<Character> iterator)
        throws IOException
    {
        try (Writer writer = new OutputStreamWriter(os))
        {
            while (iterator.hasNext())
            {
                Character ch = iterator.next();
                writer.write(ch.charValue());
            }
        }
    }
}
