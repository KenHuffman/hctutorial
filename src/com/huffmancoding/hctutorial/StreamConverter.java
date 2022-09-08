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
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;

/**
 * The Huffman Algorithm can compress different types of data. It has been used
 * to compress colors in a JPG image and sounds in MPG files. This stream
 * converter class helps the packer and unpacker read and write these objects
 * from streams.
 *
 * @author Ken Huffman
 */
public interface StreamConverter<T>
{
    /**
     * To get a predictable Huffman Tree of the objects, we need to handle when
     * two objects have the same frequency in an input. When that happens, we'll
     * choose the object that compares less that the other to appear earlier in
     * the tree. It will use this comparator.
     *
     * @return a comparator for objects themselves.
     */
    public Comparator<T> getObjectComparator();

    /**
     * Returns an interator for every object in the uncompressed input file.
     *
     * @param is the uncompressed input file to read from
     */
    public Iterator<T> inputStreamIterator(InputStream is);

    /**
     * Writes an original object as part of the serialized Huffman Tree
     * preceeds the compressed data.
     *
     * @param os the stream to write to
     * @param object the object to write (NOT as compressed bits)
     * @throws IOException if the write fails
     */
    public void writeHuffmanTreeObject(DataOutputStream os, T object) throws IOException;

    /**
     * Reads an original object from the Huffman tree the beginning of
     * the compressed.
     *
     * @param is the uncompressed input file to read from
     * @return the an uncompressed object
     * @throws IOException if the read fails
     */
    public T readHuffmanTreeObject(DataInputStream is) throws IOException;

    /**
     * Write objects to an uncompressed file from an iterator that is walking
     * the compressed bits.
     *
     * @param iterator for the objects to write (NOT as compressed bits)
     * @param os the stream to write to
     * @throws IOException if the write fails
     */
    public void writeAllToOutput(Iterator<T> iterator, OutputStream os)
        throws IOException;
}
