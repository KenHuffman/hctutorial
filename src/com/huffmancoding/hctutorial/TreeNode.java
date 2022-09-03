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

/**
 * Nodes of Huffman Tree have a frequency.
 *
 * @param <T> The type of objects from the source in the tree
 *
 * @author Ken Huffman
 */
public abstract class TreeNode<T>
{
    /** the number of times the objects in this are in the input. */
    protected int frequency;

    /**
     * Constructor.
     *
     * @param f the count of these items are in the source.
     */
    public TreeNode(int f)
    {
        frequency = f;
    }

    /**
     * Returns the number of times something in this tree has been
     * encountered in the source.
     *
     * @return the frequency.
     */
    public int getFrequency()
    {
        return frequency;
    }

    /**
     * Get leftmode object in the tree.
     *
     * @return the TreeNode's object
     */
    public abstract T getLeftmodeObject();
}
