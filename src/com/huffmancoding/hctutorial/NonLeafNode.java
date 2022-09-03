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
 * This class represents non-leaf node in the tree.
 * It has a left and right child. The children can be either a leaf node
 * or non-leaf node.
 *
 * @param <T> The type of objects from the source in the tree
 *
 * @author Ken Huffman
 */
public class NonLeafNode<T> extends TreeNode<T>
{
    /** The left side */
    private final TreeNode<T> left;

    /** The right side */
    private final TreeNode<T> right;

    /**
     * Constructor takes two child nodes it will be the parent of.
     * The frequency of this type of node is the sum of its children.
     *
     * @param left the left (zero) branch
     * @param right the right (one) branch
     */
    public NonLeafNode(TreeNode<T> l, TreeNode<T> r)
    {
        super(l.getFrequency() + r.getFrequency());

        left = l;
        right = r;
    }

    /**
     * Returns the left side of the tree.
     *
     * @return the left branch
     */
    public TreeNode<T> getLeft()
    {
        return left;
    }

    /**
     * Returns the right side of the tree.
     *
     * @return the right branch
     */
    public TreeNode<T> getRight()
    {
        return right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getLeftmodeObject()
    {
        return left.getLeftmodeObject();
    }
}
