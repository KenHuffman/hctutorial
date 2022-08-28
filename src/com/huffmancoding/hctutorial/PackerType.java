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

/**
 * This enum corresponds to the types of data that can found in an unpacked
 * file. Each value has its own FilePacker and FileUnpacker.
 *
 * If a new type of file is to be packed. Add a new enum value here, create a
 * new pair of Packer/Unpacker, and update the PackerFactory.
 *
 * @author Ken Huffman
 */
public enum PackerType
{
    /** file data that should use a Reader and Writer. */
    CHARACTER((byte)0x01),

    /** file data that should use readByte and writeByte. */
    BYTE((byte)0x02);

    /** the character that will appear at the head of the packed file. */
    byte signifier;

    /**
     * Private constructor.
     *
     * @param signifier the byte for the packed file
     */
    private PackerType(byte signifier)
    {
        this.signifier = signifier;
    }

    /**
     * Returns the byte to put in the packed file indicating Packer.
     *
     * @return the character
     */
    public byte toSignifier()
    {
        return signifier;
    }

    /**
     * Returns the enum from the byte found in the file.
     *
     * @param signifier
     * @return the enum to determine the Unpacker
     */
    public static PackerType fromSignifier(byte signifier)
    {
        for (PackerType value : values())
        {
            if (value.signifier == signifier)
            {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown signifier: " + signifier);
    }
}
