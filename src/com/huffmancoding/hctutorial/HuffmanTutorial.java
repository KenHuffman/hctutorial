package com.huffmancoding.hctutorial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
/******************************************************************************

    HuffmanTutorial: The Huffman Coding sample code.
    Copyright (C) 2002 Kenneth D. Huffman.

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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

/**
 * This program computes the Huffman coding tree for a file.
 * It expects the name of the unpacked file as an argument to the file.
 * This program packs it to a new file.
 *
 * It will then re-read the packed file to verify file that it matches
 * by comparing MD5 checksums.
 *
 * @author Ken Huffman
 */
public class HuffmanTutorial
{
    /** the boolean value for traversing left down the tree. */
    private static final boolean LEFT_NODE = false;

    /** the boolean value for traversing right down the tree. */
    private static final boolean RIGHT_NODE = true;

    /** the highest order bit of a serialized byte. */
    private static final int HIGH_BIT_OF_BYTE = 128;

    /** The number of characters in the file. */
    private int totalChars = 0;

    /** the total number of bits in the compressed file. */
    private int totalBits = 0;

    /**
     * Nodes of Huffman Tree have a frequency.
     *
     * @param <T> The type of objects from the source in the tree
     */
    static private abstract class TreeNode<T>
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
    }

    /**
     * This is a LeafNode. It does not have children, but it does wrap an
     * object (of type T) that was in the source.
     *
     * @param <T> The type of objects from the source in the tree
     */
    static private class LeafNode<T> extends TreeNode<T>
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

    /**
     * This class represents non-leaf node in the tree.
     * It has a left and right child. The children can be either a leaf node
     * or non-leaf node.
     *
     * @param <T> The type of objects from the source in the tree
     */
    static private class NonLeafNode<T> extends TreeNode<T>
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
    }

    /**
     * Packed objects are represented by a sequence of bits. Each leaf node in
     * a Huffman tree is indicated by path left (0) and right (1) from top of the
     * tree.
     */
    static private class BitArray
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

    /**
     * This is a DataOutputStream that allows individual bits to be written to
     * an OutputStream. Bits are accumulated from the highest to lowest bits of
     * a byte (128, 64, 32, 16, 8, 4, 2, 1). After 8 bits are written the byte
     * is flushed as a byte.
     */
    static private class BitOutputStream extends DataOutputStream
    {
        /** the byte as it accumulates before being written. */
        int unwrittenByte = 0;

        /** the bit mask to be written to next. */
        int bitPosition = HIGH_BIT_OF_BYTE;

        /**
         * Constructor.
         *
         * @param out the wrapped OutputStream
         */
        public BitOutputStream(OutputStream out)
        {
            super(out);
        }

        /**
         * Write a single bit within the next byte.
         *
         * @param bit the bit to write
         * @throws IOException in case of write errors.
         */
        public void writeBit(boolean bit) throws IOException
        {
            if (bit)
            {
                unwrittenByte += bitPosition;
            }

            bitPosition >>= 1;
            if (bitPosition == 0)
            {
                flush();
            }
        }

        /**
         * Write the accumulated bits to the underlying stream
         */
        @Override
        public void flush() throws IOException
        {
            if (bitPosition != HIGH_BIT_OF_BYTE)
            {
                super.writeByte(unwrittenByte);
                unwrittenByte = 0;
                bitPosition = HIGH_BIT_OF_BYTE;
            }

            super.flush();
        }
    }

    /**
     * A DataInputStream that allows individual bits to be read.
     * The underlying stream is read a byte at a time into the unreadByte
     * and the bits are returned from the highest bit to the lowest.
     */
    static private class BitInputStream extends DataInputStream
    {
        /** the byte currently being parsed. */
        int unreadByte = 0;

        /** the position within the unread byte to read next.
            0 means the next byte should be read into unreadByte first. */
        int bitPosition = 0;

        /**
         * Constructor.
         *
         * @param in the wrapped InputStream
         */
        protected BitInputStream(InputStream in)
        {
            super(in);
        }

        /**
         * Read the next bit from the InputStream.
         *
         * @return the next bit
         * @throws IOException in case of read error.
         */
        public boolean readBit() throws IOException
        {
            if (bitPosition == 0)
            {
                unreadByte = super.readByte();
                bitPosition = HIGH_BIT_OF_BYTE;
            }

            boolean bit = ((unreadByte & bitPosition) != 0);
            bitPosition >>= 1;

            return bit;
        }
    }

    /**
     * This OutputStream throws away the content.
     */
    private class NullOutputStream extends OutputStream
    {
        @Override
        public void write(int ch) throws IOException
        {
            // ignore data
        }
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
        Map<Character, LeafNode<Character>> leafNodes = new HashMap<>();

        try (InputStream is = new FileInputStream(sourceFile);
             Reader reader = new InputStreamReader(is))
        {
            // read char by char
            int i;
            while ((i = reader.read()) > 0)
            {
                LeafNode<Character> charLeaf =
                    leafNodes.computeIfAbsent((char)i, c -> new LeafNode<>(c, 0));
                charLeaf.incrementFrequency();
                ++totalChars;
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
     * @param <T> The type of Object in the in left nodes
     *
     * @param counts map of T objects to counts
     * @return a Set TreeNodes containing T objects.
     */
    private <T> NavigableSet<TreeNode<T>> createSortedSetOfLeafNodes(
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
        // will never compare equal. If a comparator returns 0, the Set assumes
        // that items are equal and only keeps one of them. This is a problem
        // because because we don't want to lose a node when we insert another
        // node with the same frequency.
        Comparator<TreeNode<T>> sortByFrequency =
            Comparator.comparingInt(TreeNode<T>::getFrequency)
                .thenComparingInt(System::identityHashCode);

        // Don't be fooled, the sortedNodes TreeSet created here is NOT a
        // Huffman Tree. A TreeSet is used here because it takes a comparator.
        // As we remove and add TreeNodes from the set, the comparator will do
        // the heavy lifting of keeping everything sorted by frequency.
        NavigableSet<TreeNode<T>> sortedNodes = new TreeSet<>(sortByFrequency);
        sortedNodes.addAll(leafNodes);

        System.out.println("Total objects: " + totalChars);
        System.out.println("Unique objects: " + sortedNodes.size());

        return sortedNodes;
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
        Map<Character, BitArray> codeByObject = new HashMap<>();

        if (tree != null)
        {
            writeSubTree(new BitArray(), tree, os, codeByObject);
        }

        System.out.println("Total bits in compressed file: " + totalBits);

        float averageCharSize = Float.NaN;
        if (totalChars != 0)
        {
            averageCharSize = (float)totalBits/(float)totalChars;
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

            writeSubTree(new BitArray(pathToObject, LEFT_NODE),
                nonLeafNode.getLeft(), os, codeByObject);
            writeSubTree(new BitArray(pathToObject, RIGHT_NODE),
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
        os.writeInt(totalChars);

        // we use an MD5 DigestInputStream when reading the sourceFile to
        // generate a checksum of the input. We'll compare it when the packed
        // data is re-read.
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(sourceFile);
             DigestInputStream is = new DigestInputStream(fis, digest);
             InputStreamReader reader = new InputStreamReader(is))
        {
            // read char by char
            int i;
            while ((i = reader.read()) > 0)
            {
                // read character and lookup its encoded bits
                Character c = (char)i;
                BitArray ba = codesByCharacter.get(c);

                for (boolean bit : ba.getBits())
                {
                    os.writeBit(bit);
                }
            }
        }

        return digest.digest();
    }

    /**
     * Read a compressed file for its original content.
     *
     * @param packedFile the compressed file
     * @return the MD5 checksum of the uncompressed file
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    private byte[] unpackFile(File packedFile)
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
            DigestOutputStream dos = new DigestOutputStream(os, digest);
            Writer writer = new OutputStreamWriter(dos))
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

                    currentNode = is.readBit() == LEFT_NODE ?
                        nonLeafNode.getLeft() : nonLeafNode.getRight();
                }

                // after reaching a leaf node, write its object.
                char ch = ((LeafNode<Character>)currentNode).getObject().charValue();
                writer.write(ch);
            }
        }

        return digest.digest();
    }

    /**
     * Main function for the application. The file to be compressed should be
     * a command line argument.
     *
     * @param args the first entry in the array should be a file name.
     */
    public static void main(String[] args)
    {
        int exitCode = 0;
        if (args.length < 1 || args[0].isEmpty())
        {
            System.err.println("Program requires a filename as a command line argument");
            exitCode = 1;
        }
        else
        {
            String filename = args[0];

            HuffmanTutorial huffman = new HuffmanTutorial();
            try
            {
                File originalFile = new File(filename).getCanonicalFile();
                File packedFile = new File(originalFile.getParentFile(), originalFile.getName() + ".packed");

                byte[] originalDigest = huffman.packFile(originalFile, packedFile);
                System.out.println("Original digest: " + DatatypeConverter.printHexBinary(originalDigest));

                byte[] unpackedDigest = huffman.unpackFile(packedFile);
                System.out.println("Unpacked digest: " + DatatypeConverter.printHexBinary(unpackedDigest));
            }
            catch (IOException | NoSuchAlgorithmException ex)
            {
                // could not read the file?
                ex.printStackTrace();
                exitCode = 1;
            }
        }
        System.exit(exitCode);
    }
}
