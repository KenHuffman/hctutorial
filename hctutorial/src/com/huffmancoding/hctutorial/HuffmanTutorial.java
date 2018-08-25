package com.huffmancoding.hctutorial;

import java.awt.FileDialog;
import java.awt.Frame;
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

    The author can be reached at www.huffmancoding.com.

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

/**
 * This program computes the Huffman coding tree for a file.
 * It will prompt for the file and pack it to a new file.
 *
 * It will then re-read the packed file to verify file that it matches
 * by comparing MD5 checksums.
 *
 * @author Ken Huffman
 */
public class HuffmanTutorial
{
    /** indicates in the serialized stream that two nodes (a left and right) follow. */
    private static final boolean IS_NONLEAFNODE = true;

    /** indicates in the serialized stream that an object from a leaf node follows. */
    private static final boolean IS_LEAFNODE = false;

    /** the boolean value for traversing left down the tree. */
    private static final boolean LEFT_NODE = false;

    /** the boolean value for traversing right down the tree. */
    private static final boolean RIGHT_NODE = true;

    /** the highest order bit of a serialized byte. */
    private static final int HIGH_BIT_OF_BYTE = 128;

    /** The number of characters in the file. */
    private int itsTotalChars = 0;

    /** the total number of bits in the compressed file. */
    private int itsTotalBits = 0;

    /**
     * Nodes of Huffman Tree have a frequency.
     */
    static private abstract class TreeNode
    {
        /** the number of times the objects in this are in the input. */
        private final int itsFrequency;

        /**
         * Constructor.
         * 
         * @param frequency the count of these items are in the source.
         */
        public TreeNode(int frequency)
        {
            itsFrequency = frequency;
        }

        /**
         * Returns the number of times something in this tree has been
         * encountered in the source.
         *
         * @return the frequency.
         */
        public int getFrequency()
        {
            return itsFrequency;
        }
    }

    /**
     * This is a LeafNode. It does not have children, but it does wrap an
     * object (of type T) that was in the source.
     *
     * @param <T> The type of objects in the source input.
     */
    static private class LeafNode<T> extends TreeNode
    {
        /** the object in the input. */
        private final T itsObject;

        /**
         * Constructor.
         *
         * @param object the object of the input.
         * @param frequency the number of times it appears in the input.
         */
        public LeafNode(T object, int frequency)
        {
            super(frequency);

            itsObject = object;
        }

        /**
         * Get the source object for this Leaf Node.
         *
         * @return the object from the source file.
         */
        public T getObject() {
            return itsObject;
        }

        /**
         * Returns the string representation of the node.
         * It is used for predictable tie-breaking when sorting.
         *
         * @return a concatenation of the left and right sides
         */
        public String getDescription()
        {
            return itsObject.getClass().getSimpleName() + " '" + itsObject.toString() + "'";
        }
    }

    /**
     * This class represents non-leaf node in the tree.
     * It has a left and right child. The children can be either a leaf node
     * or non-leaf node.
     */
    static private class NonLeafNode extends TreeNode
    {
        /** The left side */
        private final TreeNode itsLeft;

        /** The right side */
        private final TreeNode itsRight;

        /**
         * Constructor takes two child nodes it will be the parent of.
         * The frequency of this type of node is the sum of its children.
         *
         * @param left the left (zero) branch
         * @param right the right (one) branch
         */
        public NonLeafNode(TreeNode left, TreeNode right)
        {
            super(left.getFrequency() + right.getFrequency());

            itsLeft = left;
            itsRight = right;
        }

        /**
         * Returns the left side of the tree.
         *
         * @return the left branch
         */
        public TreeNode getLeft()
        {
            return itsLeft;
        }

        /**
         * Returns the right side of the tree.
         *
         * @return the right branch
         */
        public TreeNode getRight()
        {
            return itsRight;
        }
    }

    /**
     * An array of bits representing a path from the root of the Huffman tree.
     */
    static private class BitArray
    {
        /** the left-right bit path. */
        final boolean[] itsBits;

        /**
         * Constructor.
         */
        public BitArray()
        {
            itsBits = new boolean[0];
        }

        /**
         * Extend a path down the Huffman tree.
         *
         * @param parent the array to the parent
         * @param bit whether to go left or right
         */
        public BitArray(BitArray parent, boolean bit)
        {
            itsBits = Arrays.copyOf(parent.itsBits, parent.itsBits.length + 1);
            itsBits[parent.itsBits.length] = bit;
        }

        /**
         * Return the path.
         *
         * @return the path from the root
         */
        public boolean[] getBits()
        {
            return itsBits;
        }

        /**
         * Return the length from the path from the root.
         *
         * @return the length
         */
        public int length()
        {
            return itsBits.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            for (boolean bit : itsBits)
            {
                builder.append(bit ? '1': '0');
            }
            return builder.toString();
        }
    }

    /**
     * This is a DataOutputStream that allows individual bits to be written to an OutputStream.
     */
    static private class BitOutputStream extends DataOutputStream
    {
        /** the byte as it accumulates before being written. */
        int itsUnwrittenByte = 0;

        /** the bit mask to be written to next. */
        int itsBitPosition = HIGH_BIT_OF_BYTE;

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
                itsUnwrittenByte += itsBitPosition;
            }

            itsBitPosition >>= 1;
            if (itsBitPosition == 0)
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
            if (itsBitPosition != HIGH_BIT_OF_BYTE)
            {
                super.writeByte(itsUnwrittenByte);
                itsUnwrittenByte = 0;
                itsBitPosition = HIGH_BIT_OF_BYTE;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException
        {
            flush();
            super.close();
        }
    }

    /**
     * A DataInputStream that allows individual bits to be read at a time.
     */
    static private class BitInputStream extends DataInputStream
    {
        /** the byte currently being parsed. */
        int itsUnreadByte = 0;

        /** the position within the unread byte to read next. */
        int itsBitPosition = 0;

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
            if (itsBitPosition == 0)
            {
                itsUnreadByte = super.readByte();
                itsBitPosition = HIGH_BIT_OF_BYTE;
            }

            boolean bit = ((itsUnreadByte & itsBitPosition) != 0);
            itsBitPosition >>= 1;

            return bit;
        }
    }

    /**
     * This function will: Read a file and count the frequency of each character,
     * build Huffman Coding tree based on these frequencies, write a compressed
     * file containing the serialized tree followed by the encrypted content.
     *
     * @param sourceFile the original uncompressed file
     * @param packedFile the packed file with serialized Huffman tree at the front
     * @return the MD5 checksum of the sourceFile
     * @throws IOException in case of File error
     * @throws NoSuchAlgorithmException MD5 check sum not available
     */
    public byte[] packFile(File sourceFile, File packedFile) throws IOException, NoSuchAlgorithmException
    {
        Map<Character, Integer> charCounts = countCharacters(sourceFile);
        NavigableSet<TreeNode> sortedCounts = createSortedSetOfLeafNodes(charCounts);
        TreeNode huffmanTree = buildTree(sortedCounts);

        try (BitOutputStream os = new BitOutputStream(new FileOutputStream(packedFile)))
        {
            Map<Character, BitArray> codesByCharacter = writeHuffmanTree(huffmanTree, os);
            return writePackedContent(sourceFile, codesByCharacter, os);
        }
    }

    /**
     * Counts the characters in the input.
     * 
     * @param sourceFile the file to read
     * @return the map characters to their frequencies.
     * @throws IOException when the input is not readable
     * @throws NoSuchAlgorithmException MD5 check sum not available
     */
    private Map<Character, Integer> countCharacters(File sourceFile) throws IOException, NoSuchAlgorithmException
    {
        System.out.println("Analyzing file: " + sourceFile);

        // maps Characters to their counts
        Map<Character, Integer> charCounts = new HashMap<>();

        try (InputStream is = new FileInputStream(sourceFile);
             Reader reader = new InputStreamReader(is))
        {
            // read char by char
            int i;
            while ((i = reader.read()) > 0)
            {
                Character c = (char)i;

                // if c isn't in charCounts, insert the key with a count 1
                // otherwise increment the previous value by 1
                charCounts.merge(c, 1, (prev, one) -> prev+1);
                ++itsTotalChars;
            }
        }

        return charCounts;
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
    private <T> NavigableSet<TreeNode> createSortedSetOfLeafNodes(
        Map<T, Integer> counts)
    {
        // Don't be fooled, the TreeSet created here is NOT a Huffman Tree.
        // It will contain a bunch of individual TreeNodes that are kept
        // in sort order as items are added/removed by a comparator that looks
        // at each one's frequency.
        //
        // By using a SortedSet with frequency Comparator, we don't have to
        // continually sort the nodes ourselves while updating this collection.
        //
        // Unfortunately by using a Set, we have to worry about multiple
        // entries with the same frequency. When a comparator returns 0, a Set
        // assumes entries are equal and only keeps one of the objects.
        // This is a problem because because we don't want to lose a node when
        // we insert another node with the same frequency. This is why we chain
        // a tie-breaking comparator so 0 is not returned unless the two nodes
        // are really the same object (at the same address).
        //
        // (BTW, this function could have instead returned a
        // SortedMap<Integer, List<TreeNode>> but it would have made it a bit
        // more awkward adding and removing TreeNodes from it. On the whole,
        // this compound Comparator seems simpler.)
        NavigableSet<TreeNode> sortedNodes = new TreeSet<>(
            Comparator.comparingInt(TreeNode::getFrequency)
                      .thenComparingInt(System::identityHashCode));

        // convert each <T> to a LeafNode for the sorted collection.
        // TreeNodes are sorted as they are added (due to the comparator)
        counts.forEach((t, count) -> {
            LeafNode<T> leafNode = new LeafNode<>(t, count);
            System.out.println(
                leafNode.getDescription() + " has frequency=" + count);
            sortedNodes.add(leafNode);
        });

        System.out.println("Total objects: " + itsTotalChars);
        System.out.println("Unique objects: " + sortedNodes.size());

        return sortedNodes;
    }

    /**
     * Build the Huffman tree.
     *
     * At the start of this function, the collection passed in has LeafNodes
     * for every unique T encountered. This function will take the least
     * prevalent frequencies out of the collection and join them into a
     * NonLeafNode that has a combined frequency then puts it back into the
     * set.
     *
     * Because sorting is always maintained thanks to the set's comparator,
     * it is easy for this function to always find the least frequent
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
    private TreeNode buildTree(NavigableSet<TreeNode> sortedNodes)
    {
        while (sortedNodes.size() > 1)
        {
            // remove the two least frequent elements in the collection
            TreeNode first = sortedNodes.pollFirst();
            TreeNode second = sortedNodes.pollFirst();

            // put a single combined element back into the set.
            TreeNode parentNode = new NonLeafNode(first, second);
            sortedNodes.add(parentNode);
        }

        // now there is only one left in the set, return it
        return sortedNodes.pollFirst();
    }

    /**
     * Calculate the encodings for the LeafNodes in the tree.
     *
     * @param tree the Huffman tree
     * @param os the stream to serialize the Huffman Tree to
     * @return the Map of the leaf values and their BitArray from walking down the tree.
     * @throws IOException in case of write error.
     * @see #readHuffmanTree()
     */
    private Map<Character, BitArray> writeHuffmanTree(TreeNode tree,
        BitOutputStream os) throws IOException
    {
        Map<Character, BitArray> codeByObject = new HashMap<>();

        if (tree != null)
        {
            writeSubtree(new BitArray(), tree, os, codeByObject);
        }

        System.out.println("Total bits in compressed file: " + itsTotalBits);

        float averageCharSize = Float.NaN;
        if (itsTotalChars != 0)
        {
            averageCharSize = (float)itsTotalBits/(float)itsTotalChars;
        }
        System.out.println("Averge bits per object: " + averageCharSize);

        return codeByObject;
    }

    /**
     * This recursively walks down the built tree and when it gets to a
     * leaf node, dumps out the accumulated bit path to it.
     *
     * @param pathToObject a String bits down to the node
     * @param node the node to continue walking down
     * @param os the OutputStream to packed content to
     * @param codeByObject the map to build while walking the tree
     * @throws IOException in case the Huffman Tree could not be serialized
     */
    private void writeSubtree(BitArray pathToObject, TreeNode node,
        BitOutputStream os, Map<Character, BitArray> codeByObject) throws IOException
    {
        if (node instanceof NonLeafNode)
        {
            os.writeBoolean(IS_NONLEAFNODE);

            // non-leaf node, tack on a left (0) and right (1) bits,
            // and recurse
            NonLeafNode nonLeafNode = (NonLeafNode)node;

            writeSubtree(new BitArray(pathToObject, LEFT_NODE),
                nonLeafNode.getLeft(), os, codeByObject);
            writeSubtree(new BitArray(pathToObject, RIGHT_NODE),
                nonLeafNode.getRight(), os, codeByObject);
        }
        else
        {
            // leaf node, print out the code
            @SuppressWarnings("unchecked")
            LeafNode<Character> leafNode = (LeafNode<Character>)node;

            os.writeBoolean(IS_LEAFNODE);
            Character object = leafNode.getObject();
            os.writeChar(object.charValue());

            System.out.println(leafNode.getDescription() + " has code=" + pathToObject.toString());
            codeByObject.put(object, pathToObject);

            // calculate total bits for every one of these T objects
            long bitsForTheseObjects = pathToObject.length() * leafNode.getFrequency();
            itsTotalBits += bitsForTheseObjects;
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

        os.writeInt(itsTotalChars);

        // we use a MD5 digest when reading the sourceFile so we have a
        // checksum of the input to compare when the compressed data is re-read
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (DigestInputStream is = new DigestInputStream(new FileInputStream(sourceFile), digest);
             InputStreamReader reader = new InputStreamReader(is))
        {
            // read char by char
            int i;
            while ((i = reader.read()) > 0)
            {
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
        try (BitInputStream is = new BitInputStream(new FileInputStream(packedFile)))
        {
            TreeNode huffmanTree = readHuffmanTree(is);
            return readPackedContent(huffmanTree, is);
        }
    }

    /**
     * De-serialize the Huffman Tree.
     *
     * @param is the InputStream that has the Huffman Tree at the front
     * @return the Huffman Tree from the file
     * @throws IOException in case of read error
     * @see #writeHuffmanTree()
     */
    private TreeNode readHuffmanTree(BitInputStream is) throws IOException
    {
        boolean isNonLeafNode = is.readBoolean();
        if (isNonLeafNode)
        {
            TreeNode left = readHuffmanTree(is);
            TreeNode right = readHuffmanTree(is);
            return new NonLeafNode(left, right);
        }
        else
        {
            char ch = is.readChar();
            return new LeafNode<>(ch, 0);
        }
    }

    /**
     * Read the packed content that follows the Huffman tree.
     *
     * @param huffmanTree the Huffman Tree to use for decoding.
     * @param is the InputStream to read from
     * @return the MD5 checksum of the uncompressed content
     * @throws IOException in case of read error
     * @throws NoSuchAlgorithmException if MD5 not available
     * @see #writeHuffmanTree()
     */
    private byte[] readPackedContent(TreeNode huffmanTree, BitInputStream is)
        throws IOException, NoSuchAlgorithmException
    {
        // This OutputStream throws away the content, but this could be a
        // FileOutputStream if we wanted to save the uncompressed content.
        OutputStream uncompressedStream = new OutputStream()
        {
            @Override
            public void write(int b) throws IOException {
                // ignore data
            }
        };

        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (DigestOutputStream os = new DigestOutputStream(uncompressedStream, digest);
            Writer writer = new OutputStreamWriter(os))
        {
            int totalChars = is.readInt();
            for (int i = 0; i < totalChars; ++i)
            {
                TreeNode currentNode = huffmanTree;
                while (currentNode instanceof NonLeafNode)
                {
                    if (is.readBit() == LEFT_NODE)
                    {
                        currentNode = ((NonLeafNode)currentNode).getLeft();
                    }
                    else
                    {
                        currentNode = ((NonLeafNode)currentNode).getRight();
                    }
                }

                @SuppressWarnings("unchecked")
                char ch = ((LeafNode<Character>)currentNode).getObject().charValue();
                writer.write(ch);
            }
        }

        return digest.digest();
    }

    /**
     * Main function for the application. Prompts for file to read.
     *
     * @param args currently ignored
     */
    public static void main(String[] args)
    {
        FileDialog dialog = new FileDialog(new Frame(), "Select File to Analyze");
        dialog.setVisible(true);
        String filename = dialog.getFile();

        // continue with the selected file if the user did NOT cancel.
        if (filename != null)
        {
            HuffmanTutorial h = new HuffmanTutorial();
            try
            {
                File originalFile = new File(dialog.getDirectory(), filename);
                File packedFile = new File(originalFile.getParentFile(), originalFile.getName() + ".packed");

                byte[] originalDigest = h.packFile(originalFile, packedFile);
                System.out.println("Original digest: " + DatatypeConverter.printHexBinary(originalDigest));

                byte[] unpackedDigest = h.unpackFile(packedFile);
                System.out.println("Unpacked digest: " + DatatypeConverter.printHexBinary(unpackedDigest));
            }
            catch (IOException | NoSuchAlgorithmException ex)
            {
                // could not read the file?
                ex.printStackTrace();
            }
        }
        System.exit(0);
    }
}
