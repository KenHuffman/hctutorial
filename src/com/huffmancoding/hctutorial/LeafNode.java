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
 * This is a LeafNode. It does not have children, but it does wrap an
 * object (of type T) that was in the source.
 *
 * @param <T> The type of objects from the source in the tree
 *
 * @author Ken Huffman
 */
public class LeafNode<T> extends TreeNode<T>
{
    /** the object in the input. */
    private final T object;

    /**
     * Constructor.
     *
     * @param obj the object of the input.
     * @param frequency the number of times it appears in the input.
     */
    public LeafNode(T obj, int frequency)
    {
        super(frequency);

        object = obj;
    }

    /**
     * Get the source object for this Leaf Node.
     *
     * @return the object from the source file.
     */
    public T getObject() {
        return object;
    }

    /**
     * Returns the string representation of the node.
     * It is used for predictable tie-breaking when sorting.
     *
     * @return a concatenation of the left and right sides
     */
    public String getDescription()
    {
        return object.getClass().getSimpleName() + " '" + object.toString() + "'";
    }

    /**
     * Increment frequency of the node.
     */
    public void incrementFrequency()
    {
        frequency++;
    }
}
