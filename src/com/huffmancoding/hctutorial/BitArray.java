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

import java.util.Arrays;

/**
 * Packed objects are represented by a sequence of bits. Each leaf node in a
 * Huffman tree is indicated by path left (0) and right (1) from top of the
 * tree.
 *
 * @author Ken Huffman
 */
public class BitArray
{
    /** the left-right bit path. */
    final boolean[] bits;

    /**
     * Constructor.
     */
    public BitArray()
    {
        bits = new boolean[0];
    }

    /**
     * Extend a path down the Huffman tree.
     *
     * @param parent the array to the parent
     * @param bit whether to go left or right
     */
    public BitArray(BitArray parent, boolean bit)
    {
        bits = Arrays.copyOf(parent.bits, parent.bits.length + 1);
        bits[parent.bits.length] = bit;
    }

    /**
     * Return the path.
     *
     * @return the path from the root
     */
    public boolean[] getBits()
    {
        return bits;
    }

    /**
     * Return the length from the path from the root.
     *
     * @return the length
     */
    public int length()
    {
        return bits.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (boolean bit : bits)
        {
            builder.append(bit ? '1' : '0');
        }
        return builder.toString();
    }
}
