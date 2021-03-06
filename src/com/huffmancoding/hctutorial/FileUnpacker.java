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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This reads a Huffman Coding compressed file created with FilePacker and
 * uncompresses it.
 *
 * It abstract because the Huffman algorithm can apply to any type of object
 * including colors (jpg files) and sounds (mp3 files).

 * There is an old UNIX "unpack" command that used to do this.
 *
 * @param <T> The type of Object in the file.
 *
 * @author Ken Huffman
 */
abstract public class FileUnpacker<T>
{
    /** the huffman tree de-serialized from the packed file. */
    private TreeNode<T> huffmanTree;

    /**
     * This iterator walks the compressed bits section of a packed file and
     * returns the original characters in order.
     */
    private class CompressedObjectIterator implements Iterator<T>
    {
        /** the number of objects left in the iterator. */
        private int objectsRemaining;

        /** the stream to read the compressed data from. */
        private final BitInputStream bitIs;

        /**
         * Constructor.
         *
         * @param totalObjects the number of objects that should be read
         * @param is the stream to read
         */
        public CompressedObjectIterator(int totalObjects, BitInputStream is)
        {
            objectsRemaining = totalObjects;
            bitIs = is;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            return objectsRemaining > 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next() throws NoSuchElementException
        {
            try
            {
                // read bits and walk the huffmanTree until we reach a leaf node
                TreeNode<T> currentNode = huffmanTree;
                while (currentNode instanceof NonLeafNode)
                {
                    NonLeafNode<T> nonLeafNode = (NonLeafNode<T>)currentNode;

                    // arbitrarily we'll make left child the false bit
                    currentNode = ! bitIs.readBit() ?
                        nonLeafNode.getLeft() : nonLeafNode.getRight();
                }

                --objectsRemaining;
                // after reaching a leaf node, return its object.
                return ((LeafNode<T>)currentNode).getObject();
            }
            catch (IOException ex)
            {
                NoSuchElementException iterEx = new NoSuchElementException(
                    "Error reading compressed data");
                iterEx.initCause(ex);
                throw iterEx;
            }
        }
    }

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
            huffmanTree = readHuffmanTree(is);
            return readPackedContent(is);
        }
    }

    /**
     * Recursively de-serialize the Huffman Tree
     *
     * @param is the InputStream that has the Huffman Tree at the front
     * @return the Huffman Tree from the file
     * @throws IOException in case of read error
     */
    private TreeNode<T> readHuffmanTree(BitInputStream is) throws IOException
    {
        boolean isNonLeafNode = is.readBoolean();
        if (isNonLeafNode)
        {
            TreeNode<T> left = readHuffmanTree(is);
            TreeNode<T> right = readHuffmanTree(is);
            return new NonLeafNode<T>(left, right);
        }
        else
        {
            T obj = readObject(is);
            return new LeafNode<>(obj, 0);
        }
    }

    /**
     * Read the encoded data portion of the packed input stream. The unpacked
     * data is thrown away, but its MD5 checksum is computed.
     *
     * @param is the InputStream to read from
     * @return the MD5 checksum of the uncompressed content
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] readPackedContent(BitInputStream is)
        throws IOException, NoSuchAlgorithmException
    {
        int totalObjects = is.readInt();

        MessageDigest digest = MessageDigest.getInstance("MD5");

        // The os could be a FileOutputStream if we wanted to save the original content.
        try (NullOutputStream os = new NullOutputStream();
            DigestOutputStream digestOs = new DigestOutputStream(os, digest))
        {
            writeObjects(digestOs, new CompressedObjectIterator(totalObjects, is));
        }

        return digest.digest();
    }

    /**
     * Reads an original object from Huffman tree aembedded t the beginning of
     * the compressed.
     *
     * @param is the uncompressed input file to read from
     * @return the an uncompressed object
     * @throws IOException if the read fails
     */
    abstract public T readObject(DataInputStream is) throws IOException;

    /**
     * Write objects to an uncompressed file from an iterator that is walking
     * the compressed bits.
     *
     * @param os the stream to write to
     * @param iterator for the objects to write (NOT as compressed bits)
     * @throws IOException if the write fails
     */
    abstract public void writeObjects(OutputStream os, Iterator<T> iterator)
        throws IOException;
}
