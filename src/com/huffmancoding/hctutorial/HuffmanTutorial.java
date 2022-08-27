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
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * This program computes the Huffman coding tree for a file.
 *
 * It expects the name of the file as a sole argument. It will create a
 * ".packed" file when compressing the file. It will uncompress the file on the
 * command line if it ends with ".packed".
 *
 * When packing, it will then re-read the packed file to verify file that it
 * matches by comparing MD5 checksums.
 *
 * @author Ken Huffman
 */
public class HuffmanTutorial
{
    private static final String PACKED_EXTENSION = ".packed";

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
                if (filename.endsWith(PACKED_EXTENSION))
                {
                    unpackFile(filename);
                }
                else
                {
                    packFile(filename);
                }
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
     * Pack a file.
     *
     * @param filename the name of the file to pack
     * @throws IOException in case of File error
     * @throws NoSuchAlgorithmException MD5 check sum not available
     */
    private static void packFile(String filename) throws IOException, NoSuchAlgorithmException
    {
        File originalFile = new File(filename).getCanonicalFile();
        File packedFile = new File(originalFile.getParentFile(), originalFile.getName() + PACKED_EXTENSION);

        // choose a Packer based on the input
        CharacterFilePacker packer = new CharacterFilePacker();
        //ByteFilePacker packer = new ByteFilePacker();

        byte[] originalDigest = packer.packFile(originalFile, packedFile);
        System.out.println("Original digest: " + byteArrayToHex(originalDigest));

        // choose an Unpacker based on the packer above
        CharacterFileUnpacker unpacker = new CharacterFileUnpacker();
        //ByteFileUnpacker unpacker = new ByteFileUnpacker();

        byte[] unpackedDigest = unpacker.unpackFile(packedFile, null);
        System.out.println("Unpacked digest: " + byteArrayToHex(unpackedDigest));
    }

    /**
     * Unpack a packed file.
     *
     * @param filename the name of the file to unpack
     * @throws IOException in case of File error
     * @throws NoSuchAlgorithmException MD5 check sum not available
     */
    private static void unpackFile(String filename) throws IOException, NoSuchAlgorithmException
    {
        File packedFile = new File(filename).getCanonicalFile();
        String packedName = packedFile.getName();
        File originalFile = new File(packedFile.getParentFile(),
            packedName.substring(0, packedName.length()-PACKED_EXTENSION.length()));

        CharacterFileUnpacker unpacker = new CharacterFileUnpacker();
        //ByteFileUnpacker unpacker = new ByteFileUnpacker();
        unpacker.unpackFile(packedFile, originalFile);
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
