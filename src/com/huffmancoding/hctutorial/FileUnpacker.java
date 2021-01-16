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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This reads a Huffman Coding compressed file created with FilePacker and
 * uncompresses it.
 *
 * There is an old UNIX "unpack" command that used to do this.
 *
 * @author Ken Huffman
 */
public class FileUnpacker
{
    /**
     * Read a compressed file for its original content.
     *
     * @param packedFile the compressed file
     * @return the MD5 checksum of the uncompressed file
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    public byte[] unpackFile(File packedFile)
        throws IOException, NoSuchAlgorithmException
    {
        try (FileInputStream fis = new FileInputStream(packedFile);
             BitInputStream is = new BitInputStream(fis))
        {
            TreeNode<Character> huffmanTree = readHuffmanTree(is);
            return readPackedContent(huffmanTree, is);
        }
    }

        /**
     * Recursively de-serialize the Huffman Tree
     *
     * @param is the InputStream that has the Huffman Tree at the front
     * @return the Huffman Tree from the file
     * @throws IOException in case of read error
     * @see #writeHuffmanTree()
     */
    private TreeNode<Character> readHuffmanTree(BitInputStream is)
        throws IOException
    {
        boolean isNonLeafNode = is.readBoolean();
        if (isNonLeafNode)
        {
            TreeNode<Character> left = readHuffmanTree(is);
            TreeNode<Character> right = readHuffmanTree(is);
            return new NonLeafNode<Character>(left, right);
        }
        else
        {
            char ch = is.readChar();
            return new LeafNode<>(ch, 0);
        }
    }

    /**
     * Read the encoded data portion of the packed input stream. The unpacked
     * data is thrown away, but its MD5 checksum is computed.
     *
     * @param huffmanTree the Huffman Tree to use for decoding.
     * @param is the InputStream to read from
     * @return the MD5 checksum of the uncompressed content
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     * @see #writeHuffmanTree()
     */
    private byte[] readPackedContent(TreeNode<Character> huffmanTree, BitInputStream is)
        throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("MD5");

        // The os could be a FileOutputStream if we wanted to save the original content.
        try (NullOutputStream os = new NullOutputStream();
            DigestOutputStream digestOs = new DigestOutputStream(os, digest);
            Writer writer = new OutputStreamWriter(digestOs))
        {
            int totalChars = is.readInt();
            for (int i = 0; i < totalChars; ++i)
            {
                // read bits and walk the huffmanTree until we reach a leaf node
                TreeNode<Character> currentNode = huffmanTree;
                while (currentNode instanceof NonLeafNode)
                {
                    NonLeafNode<Character> nonLeafNode =
                        (NonLeafNode<Character>)currentNode;

                    // arbitrarily we'll make left child the false bit
                    currentNode = ! is.readBit() ?
                        nonLeafNode.getLeft() : nonLeafNode.getRight();
                }

                // after reaching a leaf node, write its object.
                char ch = ((LeafNode<Character>)currentNode).getObject().charValue();
                writer.write(ch);
            }
        }

        return digest.digest();
    }
}
