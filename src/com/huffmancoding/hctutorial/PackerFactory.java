package com.huffmancoding.hctutorial;

/******************************************************************************

    HuffmanTutorial: The Huffman Coding sample code.
    Copyright (C) 2022 Kenneth D. Huffman.

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * This factory class will create FilePackers and FileUnpackers for the
 * PackerType enum.
 *
 * @author Ken Huffman
 */
public class PackerFactory
{
    /**
     * Returns a PackerType that should be used by examining the contents of a
     * file.
     *
     * @param unpackedPath the path to the file to probe
     * @return The packer that should be used for the type of file
     * @throws IOException if an I/O error occurs
     */
    public PackerType getPackerType(Path unpackedPath) throws IOException
    {
        String contentType = Files.probeContentType(unpackedPath);
        System.out.println("ContentType: " + contentType);

        if (contentType.startsWith("text"))
        {
            return PackerType.CHARACTER;
        }
        else
        {
            return PackerType.BYTE;
        }
    }

    /**
     * Create a {@link FilePacker} that should be used with a particular type of file.
     *
     * @param type the type of Packer determined examining the unpacked file.
     * @return the FilePacker to use to write the compressed data
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    public FilePacker<?> getFilePacker(PackerType type)
        throws NoSuchAlgorithmException
    {
        return switch (type)
        {
            case CHARACTER -> new CharacterFilePacker();
            case BYTE -> new ByteFilePacker();
        };
    }

    /**
     * Create a {@link FileUnpacker) that should be used for a packed file
     *
     * @param type the type of Packer determine by examing the first byte of the
     * packed file
     * @return the FileUnpacker to use to read the compressed data
     * @throws NoSuchAlgorithmException if MD5 not available
     */
    public FileUnpacker<?> getFileUnpacker(PackerType type)
        throws NoSuchAlgorithmException
    {
        return switch (type)
        {
            case CHARACTER -> new CharacterFileUnpacker();
            case BYTE -> new ByteFileUnpacker();
        };
    }
}
