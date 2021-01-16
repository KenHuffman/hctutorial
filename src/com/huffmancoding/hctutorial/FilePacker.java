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
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class uses Huffman Coding to convert a file into a compressed one.
 *
 * There is an old UNIX "pack" command that used to do this.
 *
 * @author Ken Huffman
 */
public class FilePacker
{
    /** The number of objects (characters?) in the file. */
    private int totalObjects = 0;

    /** the total number of bits in the compressed file. */
    private int totalBits = 0;

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
        Collection<LeafNode<Character>> individualLeafNodes =
            createIndividualLeafNodes(sourceFile);
        NavigableSet<TreeNode<Character>> sortedNodes =
            createSortedSetOfLeafNodes(individualLeafNodes);
        TreeNode<Character> huffmanTree = mergeNodesIntoTree(sortedNodes);

        try (FileOutputStream fos = new FileOutputStream(packedFile);
             BitOutputStream os = new BitOutputStream(fos))
        {
            Map<Character, BitArray> codesByCharacter = writeHuffmanTree(huffmanTree, os);
            return writePackedContent(sourceFile, codesByCharacter, os);
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
    private Collection<LeafNode<Character>> createIndividualLeafNodes(
        File sourceFile) throws IOException, NoSuchAlgorithmException
    {
        System.out.println("Analyzing file: " + sourceFile);

        // maps Characters to their counts
        Map<Character, LeafNode<Character>> leafNodes = new TreeMap<>();

        try (InputStream is = new FileInputStream(sourceFile);
            Reader reader = new InputStreamReader(is))
        {
            int i;
            while ((i = reader.read()) > 0)
            {
                LeafNode<Character> leafNode =
                    leafNodes.computeIfAbsent((char)i, ch -> new LeafNode<>(ch, 0));
                leafNode.incrementFrequency();
                ++totalObjects;
            }
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
     * @param <T> The type of Object in the in TreeNodes
     * @param counts map of T objects to counts
     * @return a Set TreeNodes containing T objects.
     */
    private <T extends Comparable<T>> NavigableSet<TreeNode<T>> createSortedSetOfLeafNodes(
        Collection<LeafNode<T>> leafNodes)
    {
        leafNodes.forEach(leafNode -> System.out.println(
            leafNode.getDescription() + " has frequency=" + leafNode.getFrequency()));

        // The Huffman algorithm repeatedly looks for the two least frequent
        // TreeNodes and merges them into a single NonLeafNode that is "taller".
        // In order to quickly determine which TreeNodes are least frequent, we
        // will use a set with a comparator that sortsby frequency.

        // Since it is very possible that two different <T> can have the same
        // frequency, we need an arbitary tie-breaker thenCompare so two nodes
        // will never compare equal. If a comparator did return 0, the Set
        // will assume they are duplicates and only keep one of them. We don't
        // want to lose tree nodes when they have the same frequency.
        Comparator<TreeNode<T>> sortByFrequency =
            Comparator.comparingInt(TreeNode<T>::getFrequency)
                .thenComparing(this::getTreeNodeComparable);

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
     * comparing the lowest (left-most) objects.
     *
     * @param <T> the objects in the TreeNode that can be compared
     * @param treeNode the TreeNode to get the lowest object from
     * @return the lowest object
     */
    private <T extends Comparable<T>> T getTreeNodeComparable(TreeNode<T> treeNode)
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
     * @param <T> The type of Object in the in TreeNodes
     * @param sortedNodes LeafNodes for the input source; this function
     *        depletes the collection upon return.
     * @return the top of the Huffman tree, or null if sortedNodes was
     *         passed in empty
     */
    private <T> TreeNode<T> mergeNodesIntoTree(NavigableSet<TreeNode<T>> sortedNodes)
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
        return sortedNodes.pollFirst();
    }

    /**
     * Serialize a Huffman Tree to the front of the output stream so a reader
     * will be able decode the packed bits that follow. For simplicity the tree
     * is written on byte boundaries, so it doesn't have to be a BitOutputStream.
     *
     * As the leaf nodes are written, the encoding map is built which helps
     * writing the packed objects laster.
     *
     * @param tree the Huffman tree
     * @param os the stream to serialize the Huffman Tree to
     * @return the Map of the leaf values and their BitArray for encoding the data.
     * @throws IOException in case of write error.
     * @see #readHuffmanTree()
     */
    private Map<Character, BitArray> writeHuffmanTree(TreeNode<Character> tree,
        DataOutputStream os) throws IOException
    {
        // this is filled during the write of the tree
        Map<Character, BitArray> codeByObject = new TreeMap<>();

        if (tree != null)
        {
            writeSubTree(new BitArray(), tree, os, codeByObject);
        }

        System.out.println("Total bits in compressed file: " + totalBits);

        float averageCharSize = Float.NaN;
        if (totalObjects != 0)
        {
            averageCharSize = (float)totalBits/(float)totalObjects;
        }
        System.out.println("Averge bits per object: " + averageCharSize);

        return codeByObject;
    }

    /**
     * This serializes a TreeNode, recursing if the TreeNode is not a leaf node.
     * If it is a leaf node, an entry is added to the map with the BitArray
     * path from the top node.
     *
     * @param pathToObject a String bits down to the node
     * @param node the node to continue walking down
     * @param os the OutputStream to packed content to
     * @param codeByObject the map to build while walking the tree
     * @throws IOException in case the Huffman Tree could not be serialized
     * @see #readHuffmanTree(BitInputStream)
     */
    private void writeSubTree(BitArray pathToObject, TreeNode<Character> node,
        DataOutputStream os, Map<Character, BitArray> codeByObject)
        throws IOException
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
            NonLeafNode<Character> nonLeafNode = (NonLeafNode<Character>)node;

            // arbitrarily we'll make left child the false bit
            writeSubTree(new BitArray(pathToObject, false),
                nonLeafNode.getLeft(), os, codeByObject);
            writeSubTree(new BitArray(pathToObject, true),
                nonLeafNode.getRight(), os, codeByObject);
        }
        else
        {
            // for a leaf node, write the object that follows.
            LeafNode<Character> leafNode = (LeafNode<Character>)node;

            Character object = leafNode.getObject();
            os.writeChar(object.charValue());

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
     * @param codesByCharacter the Huffman tree paths for each unique Character in the input
     * @param os the packed bit stream to write to
     * @return the MD5 checksum of the original file
     * @throws IOException in case of write error
     * @throws NoSuchAlgorithmException if MD5 not available
     * @see #readPackedContent()
     */
    private byte[] writePackedContent(File sourceFile,
        Map<Character, BitArray> codesByCharacter, BitOutputStream os)
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
             DigestInputStream digestIs = new DigestInputStream(fIs, digest);
             InputStreamReader reader = new InputStreamReader(digestIs))
        {
            int i;
            while ((i = reader.read()) > 0)
            {
                Character object = (char)i;
                BitArray ba = codesByCharacter.get(object);

                for (boolean bit : ba.getBits())
                {
                    os.writeBit(bit);
                }
            }
        }

        return digest.digest();
    }
}
