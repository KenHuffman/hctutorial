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
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

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

                FilePacker packer = new FilePacker();
                byte[] originalDigest = packer.packFile(originalFile, packedFile);
                System.out.println("Original digest: " + DatatypeConverter.printHexBinary(originalDigest));

                FileUnpacker unpacker = new FileUnpacker();
                byte[] unpackedDigest = unpacker.unpackFile(packedFile);
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
