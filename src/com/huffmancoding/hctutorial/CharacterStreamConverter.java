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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;

/**
 * This reads/writes an unpacked files containing characters with a Reader.
 * It also knows how to read/write characters that are part of the serialized
 * Huffman Tree.
 *
 * @author Ken Huffman
 */
public class CharacterStreamConverter implements StreamConverter<Character>
{
    /**
     * This is an iterator for all the Characters in an original file.
     */
    private static class CharacterIterator implements Iterator<Character>
    {
        /** The reader to read for characters. */
        private final Reader reader;

        /** the char to return with the {@link #next()} call. */
        private int nextChar;

        /**
         * Constructor.
         *
         * @param is the input stream to read chars from
         */
        public CharacterIterator(InputStream is)
        {
            reader = new InputStreamReader(is);
            nextChar = readNextChar();
        }

        /**
         * {@inheritDoc}}
         */
        @Override
        public boolean hasNext()
        {
            return nextChar >= 0;
        }

        /**
         * {@inheritDoc}}
         */
        @Override
        public Character next()
        {
            char value = (char)nextChar;
            nextChar = readNextChar();
            return value;
        }

        /**
         * Read the next char to be returned next time.
         *
         * @return the char that was read, -1 if end of reader.
         */
        private int readNextChar()
        {
            int i;
            try
            {
                i = reader.read();
            }
            catch (IOException ex)
            {
                i = -1;
            }

            if (i < 0)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ex)
                {
                    throw new RuntimeException(ex);
                }
            }
            return i;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparator<Character> getObjectComparator()
    {
        return Character::compare;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Character> inputStreamIterator(InputStream is)
    {
        return new CharacterIterator(is);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeHuffmanTreeObject(DataOutputStream os, Character ch) throws IOException
    {
        os.writeChar(ch.charValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Character readHuffmanTreeObject(DataInputStream is) throws IOException
    {
        return is.readChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAllToOutput(Iterator<Character> iterator, OutputStream os)
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
