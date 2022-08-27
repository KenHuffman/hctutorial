hctutorial
==========

This Java tool is an implementation of the [Huffman Coding](https://en.wikipedia.org/wiki/Huffman_coding)
algorithm.

The old Unix [pack](https://www.unix.com/man-page/linux/1/pack/) command
used Huffman Coding, so the term is used here although the file
format of this Java code is probably not compatible with the old "C" code.

The Huffman Algorithm can apply to compressing streams of any type of object.
The code includes a input stream readers of characters and another for bytes.
Readers for other more complex object types could be implemented.

This program compiles with Java 17 or newer simply because I like the new instanceof syntax.

PROGRAM ARGUMENTS
-----------------
The program takes one command line argument: the name of the text file to pack. A ".packed" file will be created.

PROGRAM INPUT
-------------
This program is split into a FilePacker and a FileUnpacker.
The FilePacker will:

- Count the number of characters in the input.
- Build leaf nodes for each unique character with its frequency.
- Build a combined Huffman tree from the leaf nodes.
- Serialize the Huffman tree into a packed file.
- Re-read the original, computing an MD5 checksum, and appending the packed content.

The FileUnpacker will:
- Read the Huffman tree back in from the file just written.
- Read the content from the remainder of the packed file and unpacking it.
- Computes an MD5 checksum of the contents that were unpacked.

The main class will, from the command line file name argument, either call the FilePacker or FileUnpacker.

PROGRAM OUTPUT
--------------
It creates a ".packed" file from a file specified on the command line.
The original file is untouched.

It will unpack the file specified on the command line, if it ends with ".packed".

FUTURE ENHANCEMENTS
-------------------

Allow the specification of the Packer class used on the commandline.
