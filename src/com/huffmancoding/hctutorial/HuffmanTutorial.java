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

            try
            {
                File originalFile = new File(filename).getCanonicalFile();
                File packedFile = new File(originalFile.getParentFile(), originalFile.getName() + ".packed");

                // choose a Packer based on the input
                CharacterFilePacker packer = new CharacterFilePacker();
                //ByteFilePacker packer = new ByteFilePacker();

                byte[] originalDigest = packer.packFile(originalFile, packedFile);
                System.out.println("Original digest: " + byteArrayToHex(originalDigest));

                // choose an Unpacker based on the packer above
                CharacterFileUnpacker unpacker = new CharacterFileUnpacker();
                //ByteFileUnpacker unpacker = new ByteFileUnpacker();

                byte[] unpackedDigest = unpacker.unpackFile(packedFile);
                System.out.println("Unpacked digest: " + byteArrayToHex(unpackedDigest));
            }
            catch (Exception ex)
            {
                // could not read the file?
                ex.printStackTrace();
                exitCode = 1;
            }
        }
        System.exit(exitCode);
    }

    /**
     * Convert a byte digest to a string.
     *
     * @param bytes the array of bytes for the digest.
     * @return String representation of the bytes
     */
    public static String byteArrayToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
           sb.append(String.format("%02x", b));
        return sb.toString();
     }
}
