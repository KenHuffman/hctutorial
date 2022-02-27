package com.huffmancoding.hctutorial;

/******************************************************************************

    HuffmanTutorial: The Huffman Coding sample code.
    Copyright (C) 2002-2022 Kenneth D. Huffman.

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * This class uses Huffman Coding on file with characters.
 *
 * @author Ken Huffman
 */
public class CharacterFilePacker extends FilePacker<Character>
{
    /**
     * {@inheritDoc}
     */
    @Override
    protected Comparator<Character> getObjectComparator()
    {
        return Character::compare;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readObjects(InputStream is, Consumer<Character> accumulator) throws IOException
    {
        try (Reader reader = new InputStreamReader(is))
        {
            int i;
            while ((i = reader.read()) >= 0)
            {
                accumulator.accept((char)i);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeObject(DataOutputStream os, Character ch) throws IOException
    {
        os.writeChar(ch.charValue());
    }
}
