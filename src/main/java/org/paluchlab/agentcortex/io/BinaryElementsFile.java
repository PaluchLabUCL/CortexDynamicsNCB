package org.paluchlab.agentcortex.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Loads chunks of binary data, elements, in a file as byte[] chunks. The first 3 bytes indicate the length of
 * the data chunk as an integer. So the structure looks like this:
 *
 * 0, 1, 2, 3, .... DATA ...
 * 0, 1, 2, 3, .... DATA ...
 *
 * The position of the first chunk starts at 4. and the length is the length of the data not including the index.
 *
 * This class cann also be used for removing chunks.
 *
 * Created on 13/04/16.
 */
public class BinaryElementsFile implements AutoCloseable{

    /**
     * For storing the length and starting position of each 'line' or 'chunk'
     */
    final private class LineInformation{
        long position;
        int length;
        LineInformation(long pos, int len){
            position = pos;
            length = len;
        }
    }
    private List<LineInformation> lineLocs = new ArrayList<>();
    private List<LineInformation> toRemove = new ArrayList<>();

    final RandomAccessFile file;
    final SeekableByteChannel channel;

    /**
     * Opens an elements file, read only.
     * @param p
     * @throws IOException
     */
    public BinaryElementsFile(Path p) throws IOException {
        file = new RandomAccessFile(p.toFile(), "r");
        channel = file.getChannel();
    }

    /**
     * Opens an elements file.
     * @param p
     * @param writable set to be writable if chunks are to be removed.
     *
     * @throws FileNotFoundException
     */
    public BinaryElementsFile(Path p, boolean writable) throws FileNotFoundException {
        String mode = writable?"rw":"r";
        file = new RandomAccessFile(p.toFile(), mode);
        channel = file.getChannel();
    }

    /**
     * If the file is not at the end, reads the next int to find the size of the next chunk. Then reads a byte[] of that
     * size from the elements file.
     *
     * @return a chunck of data, or null if the file is at the end.
     * @throws IOException
     */
    public byte[] readNextChunk() throws IOException {
        long position = file.getFilePointer() + 4l;
        if(position>=file.length()){
            return null;
        }
        int size = file.readInt();
        byte[] chunk = new byte[size];
        file.readFully(chunk);
        lineLocs.add(new LineInformation(position, size));
        return chunk;
    }

    public void removeLastLine(){
        toRemove.add(lineLocs.get(lineLocs.size()-1));
    }

    public long getLastPosition(){
        return lineLocs.get(lineLocs.size()-1).position;
    }

    public int getLastSize(){
        return lineLocs.get(lineLocs.size()-1).length;
    }

    /**
     * Closes the file,
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        long truncated = 0;
        if(toRemove.size()>0){
            Iterator<LineInformation> iter = toRemove.iterator();
            int longest = 0;
            for(LineInformation lif: lineLocs){
                longest = lif.length+4>longest?lif.length+4:longest;
            }

            ByteBuffer buff = ByteBuffer.wrap(new byte[longest]);

            for(LineInformation information: lineLocs){
                //go through all of the lineLocs, remove if nescessary.
                if(toRemove.contains(information)){
                    //remove the line, and its length.
                    truncated += 4 + information.length;
                } else if(truncated>0){
                    //read the old bytes, and their size.
                    channel.position(information.position-4);
                    buff.limit(information.length+4);
                    channel.read(buff);
                    buff.rewind();
                    channel.position(information.position - truncated - 4);
                    channel.write(buff);
                    buff.rewind();
                }

            }

            //
            channel.truncate(channel.size()-truncated);
        }

        channel.close();
        file.close();
    }


}
