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

import java.io.DataOutputStream;
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
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

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
public abstract class FilePacker<T>
{
    /** A comparator for non-arbitrary ordering of equal frequencies in the tree. */
    private final Comparator<T> tieBreakingComparator;

    /** The number of objects (characters?) in the file. */
    private int totalObjects = 0;

    /** the total number of bits in the compressed file. */
    private int totalBits = 0;

    /** the huffman tree built from the original data. */
    private TreeNode<T> huffmanTree;

    /** the "inverted" tree with the bits for each leaf node. */
    private Map<T, BitArray> codeByObject;

    /**
     * Constructor.
     *
     * @param comparator compares T for non-arbitrary ordering.
     */
    public FilePacker(Comparator<T> comparator)
    {
        // As we are building the Huffman Tree, we keep TreeNodes in a Set.
        // Since it is very possible that two different <T> can have the same
        // frequency, we need an arbitary tie-breaker thenCompare so two nodes
        // will never compare equal. If a comparator did return 0, the Set
        // will assume they are duplicates and only keep one of them. We don't
        // want to lose tree nodes when they have the same frequency.
        tieBreakingComparator = comparator;
    }

    /**
     * This function will: Read a file and count the frequency of each character,
     * build Huffman Coding tree based on these frequencies, write a compressed
     * file containing the serialized tree followed by the encoded content.
     *
     * @param sourceFile the original unpacked file
     * @param packedFile the packed file with serialized Huffman tree at the front
     * @return the MD5 checksum of the sourceFile
     * @throws IOException in case of File error
     * @throws NoSuchAlgorithmException MD5 check sum not available
     */
    public byte[] packFile(File sourceFile, File packedFile)
        throws IOException, NoSuchAlgorithmException
    {
        Collection<LeafNode<T>> individualLeafNodes =
            createIndividualLeafNodes(sourceFile);
        NavigableSet<TreeNode<T>> sortedNodes =
            createSortedSetOfLeafNodes(individualLeafNodes);
        mergeNodesIntoTree(sortedNodes);

        try (FileOutputStream fos = new FileOutputStream(packedFile);
             BitOutputStream os = new BitOutputStream(fos))
        {
            writeHuffmanTree(os);
            return writePackedContent(sourceFile, os);
        }
    }

    /**
     * Counts the characters in the input creates a collection of LeafNodes for
     * each unique character.
     *
     * @param sourceFile the file to read
     * @return leaf nodes for each character with their frequencies.
     * @throws IOException when the input is not readable
     * @throws NoSuchAlgorithmException MD5 check sum not available
     */
    private Collection<LeafNode<T>> createIndividualLeafNodes(File sourceFile)
        throws IOException, NoSuchAlgorithmException
    {
        System.out.println("Analyzing file: " + sourceFile);

        // maps Characters to their counts
        Map<T, LeafNode<T>> leafNodes = new TreeMap<>();

        try (InputStream is = new FileInputStream(sourceFile))
        {
            readObjects(is, t -> {
                LeafNode<T> leafNode =
                    leafNodes.computeIfAbsent(t, obj -> new LeafNode<>(obj, 0));
                leafNode.incrementFrequency();
                ++totalObjects;
            });
        }

        return leafNodes.values();
    }

    /**
     * Converts a map of objects and their counts to a set of individual
     * LeafNodes that sorts by their frequency.
     *
     * Note: This function returns a NavigableSet because it has a handy
     * function for removing the first entry (with the lowest frequency)
     * which is a frequent operation in building the Huffman Tree.
     *
     * @param counts map of T objects to counts
     * @return a Set TreeNodes containing T objects.
     */
    private NavigableSet<TreeNode<T>> createSortedSetOfLeafNodes(
        Collection<LeafNode<T>> leafNodes)
    {
        leafNodes.forEach(leafNode -> System.out.println(
            leafNode.getDescription() + " has frequency=" + leafNode.getFrequency()));

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
        T obj1 = getLeftmostObject(treeNode1);
        T obj2 = getLeftmostObject(treeNode2);
        return tieBreakingComparator.compare(obj1, obj2);
    }

    /**
     * Get the "lowest" comparable T in a TreeNode
     *
     * @param treeNode the node to walk left on
     */
    private T getLeftmostObject(TreeNode<T> treeNode)
    {
        while (treeNode instanceof NonLeafNode)
        {
            treeNode = ((NonLeafNode<T>)treeNode).getLeft();
        }

        return ((LeafNode<T>)treeNode).getObject();
    }

    /**
     * Build the Huffman tree.
     *
     * At the start of this function, the collection passed in has LeafNodes
     * for every unique T encountered. This function will take the two least
     * prevalent frequencies out of the collection and joins them into a
     * NonLeafNode that has a combined frequency, then puts the new node back
     * into the set.
     *
     * Because sorting is always maintained thanks to the set's comparator,
     * it is easy for this function to always quick to find the least frequent
     * TreeNodes.
     *
     * The swapping of two TreeNodes for one continues until there is
     * only one TreeNode remaining in the collection. After the first
     * few replacements, this code will starting merging NonLeafNodes
     * instead of just merging LeafNodes. It all depends on the distribution
     * of input characters.
     *
     * Unless the input is very trivial, the final entry in the Set will be a
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

        // now there is only one left in the set, return it
        huffmanTree = sortedNodes.pollFirst();
    }

    /**
     * Serialize a Huffman Tree to the front of the output stream so a reader
     * will be able decode the packed bits that follow. For simplicity the tree
     * is written on byte boundaries, so it doesn't have to be a BitOutputStream.
     *
     * As the leaf nodes are written, the encoding map is built which helps
     * writing the packed objects laster.
     *
     * @param os the stream to serialize the Huffman Tree to
     * @throws IOException in case of write error.
     */
    private void writeHuffmanTree(DataOutputStream os)
        throws IOException
    {
        // this is filled during the write of the tree
        codeByObject = new HashMap<>();

        if (huffmanTree != null)
        {
            writeSubTree(new BitArray(), huffmanTree, os);
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
     * This serializes a TreeNode, recursing if the TreeNode is not a leaf node.
     * If it is a leaf node, an entry is added to the map with the BitArray
     * path from the top node.
     *
     * @param pathToObject a String bits down to the node
     * @param node the node to continue walking down
     * @param os the OutputStream to packed content to
     * @throws IOException in case the Huffman Tree could not be serialized
     */
    private void writeSubTree(BitArray pathToObject, TreeNode<T> node,
        DataOutputStream os) throws IOException
    {
        // When serializing each tree node, we need to preface the node's
        // data with a flag to indicate, for later reading, what type of tree
        // node and the kind of data that follows.
        // Arbitrarily we chose a true flag for a non-leaf and false for a leaf.
        boolean isNonLeafNode = (node instanceof NonLeafNode);
        os.writeBoolean(isNonLeafNode);

        if (isNonLeafNode)
        {
            // for a non-leaf node, write the left (0) and right (1) bits,
            // and recurse
            NonLeafNode<T> nonLeafNode = (NonLeafNode<T>)node;

            // arbitrarily we'll make left child the false bit
            writeSubTree(new BitArray(pathToObject, false),
                nonLeafNode.getLeft(), os);
            writeSubTree(new BitArray(pathToObject, true),
                nonLeafNode.getRight(), os);
        }
        else
        {
            // for a leaf node, write the object that follows.
            LeafNode<T> leafNode = (LeafNode<T>)node;

            T object = leafNode.getObject();
            writeObject(os, object);

            System.out.println(leafNode.getDescription() + " has code=" + pathToObject.toString());
            codeByObject.put(object, pathToObject);

            // calculate total bits for every one of these T objects
            long bitsForTheseObjects = pathToObject.length() * leafNode.getFrequency();
            totalBits += bitsForTheseObjects;
        }
    }

    /**
     * Re-read the source file and write the packed bits to an OutputStream.
     *
     * @param sourceFile the source file of characters
     * @param os the packed bit stream to write to
     * @return the MD5 checksum of the original file
     * @throws IOException in case of write error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] writePackedContent(File sourceFile, BitOutputStream os)
        throws IOException, NoSuchAlgorithmException
    {
        System.out.println("Packing file: " + sourceFile);

        // because the remainder of the file is a stream of bits that may end
        // in the middle of a byte, we write the number characters in the
        // original file before the bit stream
        os.writeInt(totalObjects);

        // we use an MD5 DigestInputStream when reading the sourceFile to
        // generate a checksum of the input. We'll compare it when the packed
        // data is re-read.
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (FileInputStream fIs = new FileInputStream(sourceFile);
             DigestInputStream digestIs = new DigestInputStream(fIs, digest))
        {
            readObjects(digestIs, obj -> {
                BitArray ba = codeByObject.get(obj);
                for (boolean bit : ba.getBits())
                {
                    try
                    {
                        os.writeBit(bit);
                    }
                    catch (IOException ex)
                    {
                        throw new RuntimeException("Could not write packed bits", ex);
                    }
                }
            });
        }

        return digest.digest();
    }

    /**
     * Reads the objects in the uncompressed input file, pass the objects to a
     * function that records the,.
     *
     * @param is the uncompressed input file to read from
     * @param accumulator the function to call with all the objects read
     * @throws IOException if the read fails
     */
    abstract public void readObjects(InputStream is, Consumer<T> accumulator)
        throws IOException;

    /**
     * Writes an original object as part of the serialized Huffman Tree
     * preceeds the compressed data.
     *
     * @param os the stream to write to
     * @param object the object to write (NOT as compressed bits)
     * @throws IOException if the write fails
     */
    abstract public void writeObject(DataOutputStream os, T object) throws IOException;
}
