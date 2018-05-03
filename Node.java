import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.LinkedList;
public class Node implements Iterable<SimpleImmutableEntry<String,Integer>> {
    public final int ADDRESS;
    public static final int ORDER = 9;
    public static final int MAX_KEYS = ORDER - 1;
    public static final int WORD_SIZE = 64;
    public static final int NODE_SIZE = MAX_KEYS * WORD_SIZE
	+ 4 * (MAX_KEYS + ORDER);
    private String[] words;
    private int[]  frequencies;
    private Node[] links;
    
    /**
     * @param addr determines where the node will be written within a file
     */
    public Node(int addr) {
	ADDRESS = addr;
	words = new String[MAX_KEYS];
	frequencies = new int[MAX_KEYS];
	for( int i : frequencies )
	    i = 0;
	links = new Node[ORDER];
    }

    // ============================  Getters  ============================

    public int getFreq( int j ) {
	return frequencies[j];
    }
    public Node getLink( int j ) {
	return links[j];
    }
    public String getWord( int j ) {
	return words[j];
    }

    public int numKeys() {
	int size = 0;
	for( int i = 0; i < MAX_KEYS ; i ++ ) {
	    if( this.getFreq(i) != 0 )
		size ++;
	    else
		break;
	}
	return size;
    }

    // ============================  Setters  ============================
    
    public Node setFreq( int j , int k ) {
	frequencies[j] = k;
	return this;
    }
    public Node setLink( int j , Node n ) {
	links[j] = n;
	return this;
    }
    public Node setWord( int j, String str ) {
	words[j] = str;
	return this;
    }

    public boolean set( int index , String word , int freq ) {
	if( canAdd(word) ) {
	    this.setWord( index , word );
	    this.setFreq( index , freq );
	    return true;
	} else {
	    return false;
	}
    }

    public static boolean canAdd( String str ) {
	byte[] bytes;
	try {
	    bytes = str.getBytes("UTF-8");
	} catch (UnsupportedEncodingException e) {
	    bytes = new byte[WORD_SIZE+1];
	    e.printStackTrace();
	}
	return bytes.length <= WORD_SIZE ;
    }

    // ===========================  File I/O  ============================

    public void writeToFile( String fileName ) {
	try{
	    RandomAccessFile outFile = new RandomAccessFile(fileName, "rw");
	    this.writeToFile( outFile );
	    outFile.close();
	} catch( IOException e ) {
	    e.printStackTrace();
	}
    }
    
    public void writeToFile( RandomAccessFile outFile ) throws IOException {
	this.updateOnFile( 0, outFile );
    }

    /**
     * @param start The data at this index in the node and everything to the
     * right of it will be updated on file
     */
    public void updateOnFile( int start, RandomAccessFile outFile )
	throws IOException {

	int offset = this.ADDRESS * NODE_SIZE;
	outFile.seek( offset + start * 4 );
	byte[] wordBytes;
	
	for( int i = start ; i < MAX_KEYS ; i++ ){
	    outFile.writeInt( frequencies[i] );
	}

	// Write the words to file
	offset += MAX_KEYS * 4;
	outFile.seek( offset + start * WORD_SIZE );
	for( int i = start ; i < MAX_KEYS ; i++ ) {
	    try {
		if( words[i] != null )
		    wordBytes = words[i].getBytes("UTF-8");
		else{
		    wordBytes = new byte[1];
		    wordBytes[0] = 0;
		}
	    } catch (UnsupportedEncodingException e) {
		wordBytes = new byte[1];
		wordBytes[0] = 0;
		e.printStackTrace();
	    }
	    outFile.write(wordBytes);
	    for( int j = wordBytes.length ; j < WORD_SIZE ; j++ )
		outFile.writeByte( 0 );
	}
	    
	// Write the links to file
	offset += MAX_KEYS * WORD_SIZE;
	outFile.seek( offset + start * 4 );
	int addrInt = -1;
	for( int i = start ; i < ORDER ; i++ ) {
	    if( links[i] != null )
		addrInt = links[i].ADDRESS;
	    else
		addrInt = -1;
	    outFile.writeInt(addrInt);
	}
    }
    
    public static Node readFromFile( int thisAddr , RandomAccessFile file )
	throws IOException {
	
	Node node = new Node(thisAddr);
	Node child;
	byte[] wordBytes = new byte[WORD_SIZE];
	String wordString = new String();
	int addrInt = 0;
	int numWords = MAX_KEYS;
	int offset = thisAddr * NODE_SIZE;
	file.seek( offset );

	// Read frequencies and words from file
	for( int i = 0 ; i < MAX_KEYS ; i++ ) {
	    node.setFreq( i, file.readInt() );
	    if( node.getFreq(i) == 0 ) {
		numWords = i;
		file.seek(offset + 4 * MAX_KEYS);
		break;
	    }
	}
	for( int i = 0 ; i < numWords ; i++ ) {
	    file.read( wordBytes , 0 , WORD_SIZE );
	    wordString = new String( wordBytes, "UTF-8" );
	    int wordLength = wordString.indexOf("\0");
	    wordString = wordString.
	    	substring(0, wordLength==-1 ? WORD_SIZE : wordLength);
	    node.setWord( i , wordString );
	}

	offset += 4 * MAX_KEYS + MAX_KEYS * WORD_SIZE;
	
	// Read links from file
	for( int i = 0 ; i < numWords+1 ; i++ ) {
	    file.seek( offset + 4 * i );
	    addrInt = file.readInt();
	    if( addrInt != -1 ) {
		node.setLink(i, Node.readFromFile(addrInt, file) );
	    }
	}
	return node;
    }
    
    // ============================   Other   ============================
    
    public boolean isFull() {
	return ( frequencies[MAX_KEYS - 1] != 0 );
    }
    public boolean isLeaf() {
	return ( links[0] == null );
    }

    /**
     * @return {@code True} if the input string occurs as any entry within
     * the node, {@code False} otherwise
     */
    public boolean contains( String str ) {
	for( int i = 0 ; i < MAX_KEYS ; i++ )
	    if( str.equals( this.getWord( i ) ) )
		return true;
	return false;
    }

    /**
     * makes everything to the right of {@code splitpoint} get shifted one slot
     * to the right, leaving the stuff at {@code splitpoint} empty
     */
    public void shiftRight( int splitPoint ) {
	for( int i = MAX_KEYS-1 ; i > splitPoint ; i -- ) {
	    words[i] = words[i-1];
	    frequencies[i] = frequencies[i-1];
	}
	for( int i = ORDER-1 ; i > splitPoint ; i -- ) {
	    links[i] = links[i-1];
	}
    }
    
    public void printNode() {
    	System.out.print( "Node " + ADDRESS );
    	for( int i = 0; i < MAX_KEYS ; i++ )
    	    System.out.println( i +  ")  (x" + this.getFreq(i) + ")  "
    				+ (this.getWord(i)==null?"":this.getWord(i)));
    	System.out.print("\n");
    }

    public Iterator<SimpleImmutableEntry<String,Integer>> iterator() {
    	LinkedList<SimpleImmutableEntry<String,Integer>> list
	    = new LinkedList<SimpleImmutableEntry<String,Integer>>();
	for( int i = 0 ; i < MAX_KEYS ; i++ ) {
	    if( this.getFreq( i ) > 0 )
		list.add(new SimpleImmutableEntry<String,Integer>(
							  this.getWord(i),
							  this.getFreq(i)));
	    else
		break;
	}
	return list.iterator();
    }
}
