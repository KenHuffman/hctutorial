hctutorial
==========

This Java tool is an implementation of the [Huffman Coding](https://en.wikipedia.org/wiki/Huffman_coding)
algorithm.

The old Unix [pack](https://www.unix.com/man-page/all/1/pack) command
used Huffman Coding, so the term is used here although the file 
format of this Java code is not compatible with the old "C" code.

The Huffman Algorithm can apply to compressing streams of any type of object.
The code includes a input stream readers of characters and another for bytes.
Readers for other more complex object types could be implemented.

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

The main class will display the before and after MD5 checksums

PROGRAM OUTPUT
--------------
It creates a ".packed" file.

While it unpacks the packed file in memory to verify that it was created
correctly, it does not create an unpacked file on disk from a .packed file.

FUTURE ENHANCEMENTS
-------------------

Allow the program to take command line arguments to indicate whether to pack
or unpack. Also allow the specification of the Packer class used on the commandline.
