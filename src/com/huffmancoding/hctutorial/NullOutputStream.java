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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This OutputStream throws away the content.
 *
 * @author Ken Huffman
 */
public class NullOutputStream extends OutputStream
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int ch) throws IOException
    {
        // ignore data
    }
}
