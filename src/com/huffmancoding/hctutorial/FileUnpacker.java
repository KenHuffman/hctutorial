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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
public class FileUnpacker<T>
{
    /** the converter that can read and write objects. */
    private final StreamConverter<T> converter;

    /** the huffman tree de-serialized from the packed file. */
    private TreeNode<T> huffmanTree;

    /**
     * Constructor.
     *
     * @param toUnpackedConverter the converter that know how to read objects
     * from the original file.
     */
    protected FileUnpacker(StreamConverter<T> toUnpackedConverter)
    {
        converter = toUnpackedConverter;
    }

    /**
     * This iterator walks the compressed bits section of a packed file and
     * returns the original characters in order.
     */
    private class CompressedObjectIterator implements Iterator<T>
    {
        /** the packed stream containing the Huffman tree and data to decode. */
        private final BitInputStream packedStream;

        /** the number of objects left in the iterator. */
        private int objectsRemaining;

        /**
         * Constructor.
         *
         * @param totalObjects the number of objects that should be read
         * @param bitIs the
         */
        public CompressedObjectIterator(int totalObjects, BitInputStream bitIs)
        {
            packedStream = bitIs;
            objectsRemaining = totalObjects;
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
                while (currentNode instanceof NonLeafNode<T> nonLeafNode)
                {
                    // arbitrarily we'll make left child the false bit
                    currentNode = ! packedStream.readBit() ?
                        nonLeafNode.getLeft() : nonLeafNode.getRight();
                }

                --objectsRemaining;
                // after reaching a leaf node, return its object.
                if (currentNode instanceof LeafNode<T> leafNode)
                {
                    return leafNode.getObject();
                }

                // if we got here, the huffmanTree contains strangeness
                throw new RuntimeException("Unknown TreeNode type: " + currentNode.getClass().getName());
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
     * @param destFile the file to unpack to
     * @return the MD5 checksum of the uncompressed file
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    public static byte[] unpackFile(File packedFile, File destFile)
        throws IOException, NoSuchAlgorithmException
    {
        System.out.println("Unpacking file: " + packedFile);

        try (FileInputStream fis = new FileInputStream(packedFile);
             BitInputStream is = new BitInputStream(fis))
        {
            ConverterType type = readConverterType(is);

            PackerFactory factory = new PackerFactory();
            FileUnpacker<?> unpacker = factory.getFileUnpacker(type);
            return unpacker.unpackStream(is, destFile);
        }
    }

    /**
     * Read the byte at the front of the file that indicates the type of
     * StreamConverter used to pack the original file.
     *
     * @param packedStream the begining of the stream of packed file
     */
    private static ConverterType readConverterType(BitInputStream packedStream)
        throws IOException
    {
        ConverterType type = ConverterType.fromSignifier(packedStream.readByte());
        System.out.println("PackerType: " + type.name());
        return type;
    }

    /**
     * Read the persisted HuffmanTree then unpack the compress data that follows.
     *
     * @param packedStream the stream to read from and unpack
     * @param destFile the file to write the original data to, can be null
     * @return the MD5 digest of the uncompressed data
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] unpackStream(BitInputStream packedStream, File destFile)
        throws IOException, NoSuchAlgorithmException
    {
        huffmanTree = readHuffmanTree(packedStream);
        return readPackedContent(packedStream, destFile);
    }

    /**
     * Recursively de-serialize the front of {@link #packedStream} to a Huffman
     * Tree.
     *
     * @param packedStream the stream to read the Huffman Tree from
     * @return the Huffman Tree from the file
     * @throws IOException in case of read error
     */
    private TreeNode<T> readHuffmanTree(BitInputStream packedStream) throws IOException
    {
        boolean isNonLeafNode = packedStream.readBoolean();
        if (isNonLeafNode)
        {
            TreeNode<T> left = readHuffmanTree(packedStream);
            TreeNode<T> right = readHuffmanTree(packedStream);
            return new NonLeafNode<T>(left, right);
        }
        else
        {
            T obj = converter.readHuffmanTreeObject(packedStream);
            return LeafNode.create(obj);
        }
    }

    /**
     * Read the encoded data portion of {@link #packedStream}. The unpacked
     * data is written to the destFile if it is not nill. The digest is updated
     * from the unpacked data.
     *
     * @param packedStream the stream to read the bits from
     * @param destFile the file to write the unpacked (original) content to,
     *   can be null if no file write is desired
     * @return the MD5 digest of the uncompressed data
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] readPackedContent(BitInputStream packedStream, File destFile) throws IOException, NoSuchAlgorithmException
    {
        int totalObjects = packedStream.readInt();

        MessageDigest digest = MessageDigest.getInstance("MD5");

        // The os could be a FileOutputStream if we wanted to save the original content.
        try (OutputStream os = destFile == null ?
                new NullOutputStream() : new FileOutputStream(destFile);
            DigestOutputStream digestOs = new DigestOutputStream(os, digest))
        {
            CompressedObjectIterator iterator = new CompressedObjectIterator(totalObjects, packedStream);
            converter.writeAllToOutput(iterator, digestOs);
        }

        return digest.digest();
    }
}
