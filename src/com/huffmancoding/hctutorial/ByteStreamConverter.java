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
     * This is an Iterator for all the Bytes in an original file.
     */
    private static class ByteIterator implements Iterator<Byte>
    {
        /** The stream to read for bytes. */
        private InputStream is;

        /** the byte to return with the {@link #next()} call. */
        private int nextByte;

        /**
         * Constructor.
         *
         * @param bytesIs the input stream to read bytes from
         */
        public ByteIterator(InputStream bytesIs)
        {
            is = bytesIs;
            nextByte = readNextByte();
        }

        /**
         * {@inheritDoc}}
         */
        @Override
        public boolean hasNext()
        {
            return nextByte >= 0;
        }

        /**
         * {@inheritDoc}}
         */
        @Override
        public Byte next()
        {
            return (byte)(nextByte > Byte.MAX_VALUE ? nextByte - 256 : nextByte); // -128 to 127
        }

        /**
         * Read the next byte to be returned next time.
         *
         * @return the byte that was read, -1 if end of stream
         */
        private int readNextByte()
        {
            int i;
            try
            {
                i = is.read();
            }
            catch (IOException ex)
            {
                i = -1;
            }

            return i;
        }
    }

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
    public Iterator<Byte> inputStreamIterator(InputStream is)
    {
        return new ByteIterator(is);
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
