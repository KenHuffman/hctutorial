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
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class uses Huffman Coding to convert a file into a compressed one.
 *
 * It abstract because the Huffman algorithm can apply to any type of object
 * including colors (e.g. jpg files) and sounds (e.g. mp3 files).
 *
 * There is an old UNIX "pack" command that used to do this for bytes.
 *
 * @param <T> The type of Object in the file.
 *
 * @author Ken Huffman
 */
public class FilePacker<T>
{
    /** the file with the original content. */
    private final File sourceFile;

    /** the class that knows how to read the objects from the #sourceFile. */
    private final StreamConverter<T> converter;

    /** the individual leaf nodes with counds by comparable objects. */
    private final Map<T, LeafNode<T>> objectCounts = new TreeMap<>();

    /** The number of objects (characters?) in the file. */
    private int totalObjects = 0;

    /** the total number of bits in the compressed file. */
    private int totalBits = 0;

    /** the huffman tree built from the original data. */
    private TreeNode<T> huffmanTree;

    /** the "inverted" tree with the bits for each leaf node. */
    private Map<T, BitArray> codeByObject;

    /**
     * Initialize FilePacker with input file.
     *
     * @param inputFile the file to pack.
     * @param toPackedConverter the converter than knows how to read T objects
     */
    protected FilePacker(File inputFile, StreamConverter<T> toPackedConverter)
    {
        sourceFile = inputFile;
        converter = toPackedConverter;
    }

    /**
     * This function will: Read a file and count the frequency of each character,
     * build Huffman Coding tree based on these frequencies, write a compressed
     * file containing the serialized tree followed by the encoded content.
     *
     * @param inputFile the original unpacked file
     * @param packedFile the packed file with serialized Huffman tree at the front
     * @return the MD5 checksum of the sourceFile
     * @throws IOException in case of File error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    public static byte[] packFile(File inputFile, File packedFile)
        throws IOException, NoSuchAlgorithmException
    {
        System.out.println("Packing file: " + inputFile);

        PackerFactory factory = new PackerFactory();
        ConverterType type = factory.probeConverterType(inputFile.toPath());
        FilePacker<?> packer = factory.createFilePacker(type, inputFile);

        try (FileOutputStream fos = new FileOutputStream(packedFile);
             BitOutputStream os = new BitOutputStream(fos))
        {
            // write breadcrumb so we know which unpacker to use
            writeConverterType(type, os);

            return packer.packStream(os);
        }
    }

    /**
     * Write a byte at the front of the file that indicates the type of
     * StreamConverter was used, so it knows how to unpack it.
     *
     * @param type the type of StreamConverter that will be used for packing
     * @param packedStream the output stream for the packed content
     * @throws IOException is case of write error
     */
    private static void writeConverterType(ConverterType type, BitOutputStream packedStream)
        throws IOException
    {
        packedStream.writeByte(type.toSignifier());
    }

    /**
     * Write the HuffmanTree followed by the compressed data.
     *
     * @param packedStream the stream to write the packed data to.
     * @return the MD5 digest of the uncompressed data
     * @throws IOException in case of write error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] packStream(BitOutputStream packedStream)
        throws IOException, NoSuchAlgorithmException
    {
        createIndividualLeafNodes();
        NavigableSet<TreeNode<T>> sortedNodes = createSortedSetOfLeafNodes();
        mergeNodesIntoTree(sortedNodes);

        writeHuffmanTree(packedStream);
        return writePackedContent(packedStream);
    }

    /**
     * Counts the characters in the input fills the {@link #individualLeafNodes}
     * for each unique character.
     *
     * @throws IOException when the input is not readable
     */
    private void createIndividualLeafNodes()
        throws IOException
    {
        System.out.println("Analyzing file: " + sourceFile);

        try (InputStream is = new FileInputStream(sourceFile))
        {
            Iterator<T> iterator = converter.inputStreamIterator(is);
            iterator.forEachRemaining(this::addObjecToIndividualLeafNodes);
        }
    }

    /**
     * Increment the occurence count of an object in the {@link #individualLeafNodes},
     * growing the map if necessary.
     *
     * @param object the object from input file
     */
    private void addObjecToIndividualLeafNodes(T object)
    {
        LeafNode<T> leafNode = objectCounts.computeIfAbsent(object, LeafNode::create);
        leafNode.incrementFrequency();
        ++totalObjects;
    }

    /**
     * Converts a map of objects and their counts to a set of individual
     * LeafNodes that sorts by their frequency.
     *
     * Note: This function returns a NavigableSet because it has a handy
     * function for removing the first entry (with the lowest frequency)
     * which is a frequent operation in building the Huffman Tree.
     *
     * @return a Set TreeNodes containing T objects.
     */
    private NavigableSet<TreeNode<T>> createSortedSetOfLeafNodes()
    {
        Collection<LeafNode<T>> leafNodes = objectCounts.values();

        leafNodes.forEach(LeafNode::dump);

        // The Huffman algorithm repeatedly looks for the two least frequent
        // TreeNodes and merges them into a single NonLeafNode that is "taller".
        // In order to quickly determine which TreeNodes are least frequent, we
        // will use a set with a comparator that sorts by frequency.
        Comparator<TreeNode<T>> sortByFrequency =
            Comparator.comparingInt(TreeNode<T>::getFrequency)
                .thenComparing(this::getTieBreakerComparator);

        // Don't be fooled, the sortedNodes TreeSet created here is NOT a
        // Huffman Tree. A TreeSet is used here because it takes a comparator.
        // As we remove and add TreeNodes from the set, the comparator will do
        // the heavy lifting of keeping everything sorted by frequency.
        NavigableSet<TreeNode<T>> sortedNodes = new TreeSet<>(sortByFrequency);
        sortedNodes.addAll(leafNodes);

        System.out.println("Total objects: " + totalObjects);
        System.out.println("Unique objects: " + sortedNodes.size());

        return sortedNodes;
    }

    /**
     * The simplest way to order TreeNodes, after their frequency, is by
     * comparing the objects in the nodes.
     *
     * @param treeNode1 the TreeNode to compare
     * @param treeNodes the TreeNode to compare against
     * @return the comparison value
     */
    private int getTieBreakerComparator(TreeNode<T> treeNode1, TreeNode<T> treeNode2)
    {
        Comparator<T> objectComparator = converter.getObjectComparator();
        T obj1 = treeNode1.getLeftmodeObject();
        T obj2 = treeNode2.getLeftmodeObject();
        return objectComparator.compare(obj1, obj2);
    }

    /**
     * Build the Huffman tree.
     *
     * At the start of this function, the collection passed in has LeafNodes
     * for every unique T encountered. This function will take the two least
     * prevalent frequencies out of the collection and join them into a
     * NonLeafNode that has a combined frequency, then puts the new node back
     * into the set.
     *
     * Because sorting is always maintained thanks to the set's comparator,
     * it is easy for this function to always quick to find the least frequent
     * TreeNodes.
     *
     * The swapping of two smaller TreeNodes for one combined TreeNode continues
     * until there is only one TreeNode remaining in the collection that has
     * everything. After the first few replacements, this code will starting
     * merging NonLeafNodes instead of just merging LeafNodes. It all depends on
     * the distribution of input objects.
     *
     * Unless the input is empty, the final entry in the Set will be a
     * NonLeafNode that has NonLeafNodes underneath it. The Huffman Tree!
     *
     * @param sortedNodes LeafNodes for the input source; this function
     *        depletes the collection upon return.
     */
    private void mergeNodesIntoTree(NavigableSet<TreeNode<T>> sortedNodes)
    {
        while (sortedNodes.size() > 1)
        {
            // remove the two least frequent elements in the collection
            TreeNode<T> first = sortedNodes.pollFirst();
            TreeNode<T> second = sortedNodes.pollFirst();

            // put a single combined element back into the set.
            TreeNode<T> parentNode = new NonLeafNode<T>(first, second);
            sortedNodes.add(parentNode);
        }

        // now there is only one left in the set, return it as the top of the tree
        huffmanTree = sortedNodes.pollFirst();
    }

    /**
     * Serialize a Huffman Tree to the front of {@link #packedStream} so a reader
     * will be able decode the packed bits that follow. For simplicity the tree
     * is written on byte boundaries, so it doesn't have to be a BitOutputStream.
     *
     * As the leaf nodes are written, the encoding map is built which helps
     * writing the packed objects laster.
     *
     * @param packedStream the stream to serialize the Huffman Tree to
     * @throws IOException in case of write error.
     */
    private void writeHuffmanTree(BitOutputStream packedStream) throws IOException
    {
        // this is filled during the write of the tree
        codeByObject = new HashMap<>();

        if (huffmanTree != null)
        {
            writeSubTree(new BitArray(), huffmanTree, packedStream);
        }

        System.out.println("Total bits in compressed file: " + totalBits);

        float averageCharSize = Float.NaN;
        if (totalObjects != 0)
        {
            averageCharSize = (float)totalBits/(float)totalObjects;
        }
        System.out.println("Averge bits per object: " + averageCharSize);
    }

    /**
     * This serializes a TreeNode to {@link #packedStream}, recursing if the
     * TreeNode is not a leaf node. If it is a leaf node, an entry is added to
     * the map with the BitArray path from the top node.
     *
     * @param pathToObject a String bits down to the node
     * @param node the node to continue walking down
     * @param packedStream the stream to serialize a TreeNode to.
     * @throws IOException in case the Huffman Tree could not be serialized
     */
    private void writeSubTree(BitArray pathToObject, TreeNode<T> node,
        BitOutputStream packedStream) throws IOException
    {
        // When serializing each tree node, we need to preface the node's
        // data with a flag to indicate, for later reading, what type of tree
        // node and the kind of data that follows.
        // Arbitrarily we chose a true flag for a non-leaf and false for a leaf.
        if (node instanceof NonLeafNode<T> nonLeafNode)
        {
            packedStream.writeBoolean(true);

            // for a non-leaf node, write the left (0) and right (1) bits,
            // and recurse (arbitrarily we'll make left child the false bit)
            writeSubTree(new BitArray(pathToObject, false),
                nonLeafNode.getLeft(), packedStream);
            writeSubTree(new BitArray(pathToObject, true),
                nonLeafNode.getRight(), packedStream);
        }
        else if (node instanceof LeafNode<T> leafNode)
        {
            packedStream.writeBoolean(false);

            // for a leaf node, write the object that follows.
            T object = leafNode.getObject();
            converter.writeHuffmanTreeObject(packedStream, object);

            System.out.println(leafNode.getDescription() + " has code=" + pathToObject.toString());
            codeByObject.put(object, pathToObject);

            // calculate total bits for every one of these T objects
            long bitsForTheseObjects = pathToObject.length() * leafNode.getFrequency();
            totalBits += bitsForTheseObjects;
        }
        else {
            // if we got here, the node is corrupt
            throw new RuntimeException("Unknown TreeNode type: " + node.getClass().getName());
        }
    }

    /**
     * Re-read the source file and write the packed bits to {@link #packedStream}.
     * The digest is updated from the input as it is read.
     *
     * @param packedStream the stream to the bits for each object in the original file.
     * @return the MD5 digest of the unpacked data as it was read
     * @throws IOException in case of write error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] writePackedContent(BitOutputStream packedStream)
        throws IOException, NoSuchAlgorithmException
    {
        // because the remainder of the file is a stream of bits that may end
        // in the middle of a byte, we write the number characters in the
        // original file before the bit stream
        packedStream.writeInt(totalObjects);

        MessageDigest digest = MessageDigest.getInstance("MD5");

        // we use an MD5 DigestInputStream when reading the sourceFile to
        // generate a checksum of the input. We'll compare it when the packed
        // data is re-read.
        try (FileInputStream fIs = new FileInputStream(sourceFile);
             DigestInputStream digestIs = new DigestInputStream(fIs, digest))
        {
            Iterator<T> iterator = converter.inputStreamIterator(digestIs);
            while (iterator.hasNext())
            {
                writeObjectBits(iterator.next(), packedStream);
            }
        }

        return digest.digest();
    }

    /**
     * Write the bits for an object to encoded portion of the packed file.
     *
     * @param object the object to write bits for.
     * @param packedStream the stream to the compress bits for an ojbect.
     */
    private void writeObjectBits(T object, BitOutputStream packedStream)
    {
        BitArray ba = codeByObject.get(object);
        for (boolean bit : ba.getBits())
        {
            try
            {
                packedStream.writeBit(bit);
            }
            catch (IOException ex)
            {
                throw new RuntimeException("Could not write packed bits", ex);
            }
        }
    }
}
